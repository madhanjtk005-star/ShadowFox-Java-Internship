# Employee Leave Management System (Spring Boot + MySQL)

REST API for employees to apply for leave and admins to approve/reject it.
Stack: Java 17, Spring Boot 3.2, Spring Data JPA, Bean Validation, MySQL 8.

## 1. Setup

1. Install JDK 17+, Maven 3.8+, MySQL 8.
2. Edit `src/main/resources/application.properties` and set `spring.datasource.username` / `password`.
   The database `leave_management` is created automatically (`createDatabaseIfNotExist=true`).
3. Run:
   ```bash
   mvn spring-boot:run
   ```
4. API is at `http://localhost:8080`. Tables are auto-created by Hibernate (`ddl-auto=update`).

On first start three users are seeded:

| id | name           | role     | leave balance |
|----|----------------|----------|---------------|
| 1  | Alice Admin    | ADMIN    | 24            |
| 2  | Bob Employee   | EMPLOYEE | 20            |
| 3  | Carol Employee | EMPLOYEE | 15            |

## 2. Identifying the caller

To keep the focus on layered architecture and business rules, the "logged-in user" is passed in the
**`X-User-Id`** request header. Roles are verified in the service layer (`403 Forbidden` for violations).
In production, replace this with Spring Security + JWT.

## 3. Schema / ER diagram

```
┌──────────────────────┐ 1            * ┌────────────────────────────┐
│ users                │────────────────│ leave_requests             │
├──────────────────────┤  employee_id   ├────────────────────────────┤
│ id PK                │                │ id PK                      │
│ name                 │ 1            * │ employee_id FK -> users.id │
│ email UNIQUE         │────────────────│ reviewed_by FK -> users.id │
│ role (EMPLOYEE/ADMIN)│  reviewed_by   │ leave_type                 │
│ leave_balance        │                │ start_date, end_date, days │
└──────────────────────┘                │ reason, status             │
                                        │ admin_remarks              │
                                        │ created_at, reviewed_at    │
                                        └────────────────────────────┘
```

Status lifecycle: `PENDING → APPROVED | REJECTED | CANCELLED` (only `PENDING` can change state).

## 4. Endpoints

| Method | URL                               | Who      | Description                          | Success |
|--------|-----------------------------------|----------|--------------------------------------|---------|
| POST   | `/api/users`                      | ADMIN    | Create a user                        | 201     |
| GET    | `/api/users/me`                   | any      | Profile + remaining balance          | 200     |
| POST   | `/api/leaves`                     | any      | Apply for leave                      | 201     |
| GET    | `/api/leaves/my`                  | any      | Own leave history                    | 200     |
| GET    | `/api/leaves/{id}`                | owner/ADMIN | One leave request                 | 200     |
| PATCH  | `/api/leaves/{id}/cancel`         | owner    | Cancel own PENDING request           | 200     |
| GET    | `/api/admin/leaves?status=`       | ADMIN    | All requests (optional status filter)| 200     |
| GET    | `/api/admin/leaves/pending`       | ADMIN    | Pending requests                     | 200     |
| PATCH  | `/api/admin/leaves/{id}/approve`  | ADMIN    | Approve (deducts balance)            | 200     |
| PATCH  | `/api/admin/leaves/{id}/reject`   | ADMIN    | Reject (no deduction)                | 200     |

Errors: `400` validation / business rule, `403` wrong role or not the owner, `404` not found.

## 5. Business rules enforced

- `startDate` not in the past; `endDate` on/after `startDate`.
- No overlap with the employee's `PENDING` or `APPROVED` leaves.
- Requested days (inclusive, calendar days) must not exceed the leave balance.
- Balance is deducted **only when an admin approves**; re-checked at approval time.
- Only `ADMIN` can approve/reject; only `PENDING` requests can be approved/rejected/cancelled.

## 6. Sample cURL (use dates in the future)

```bash
# Employee Bob applies for leave
curl -X POST http://localhost:8080/api/leaves \
  -H "Content-Type: application/json" -H "X-User-Id: 2" \
  -d '{"leaveType":"ANNUAL","startDate":"2026-12-22","endDate":"2026-12-26","reason":"Family trip"}'

# Bob's history
curl http://localhost:8080/api/leaves/my -H "X-User-Id: 2"

# Admin views pending requests
curl http://localhost:8080/api/admin/leaves/pending -H "X-User-Id: 1"

# Admin approves with remarks
curl -X PATCH http://localhost:8080/api/admin/leaves/1/approve \
  -H "Content-Type: application/json" -H "X-User-Id: 1" \
  -d '{"remarks":"Enjoy your trip"}'

# Admin rejects
curl -X PATCH http://localhost:8080/api/admin/leaves/2/reject \
  -H "Content-Type: application/json" -H "X-User-Id: 1" \
  -d '{"remarks":"Team deadline that week"}'

# Employee tries admin action -> 403
curl -X PATCH http://localhost:8080/api/admin/leaves/1/approve -H "X-User-Id: 2"

# Admin creates a user
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" -H "X-User-Id: 1" \
  -d '{"name":"Dan","email":"dan@company.com","role":"EMPLOYEE","leaveBalance":18}'
```

Sample error response:

```json
{
  "timestamp": "2026-11-14T10:15:30.123",
  "status": 400,
  "error": "Bad Request",
  "message": "Leave dates overlap with an existing PENDING or APPROVED leave request"
}
```

Validation errors add a `fieldErrors` map, e.g. `{"startDate":"startDate cannot be in the past"}`.

## 7. Testing on Windows PowerShell

`curl` is an alias in Windows PowerShell, so use this helper instead (paste once per session):

```powershell
function Call($m,$u,$id,$b){
  $p=@{Method=$m;Uri="http://localhost:8080$u";ContentType="application/json"}
  if($id){$p.Headers=@{"X-User-Id"=$id}}
  if($b){$p.Body=$b}
  try{ Invoke-RestMethod @p }
  catch{
    "HTTP " + [int]$_.Exception.Response.StatusCode
    $msg = $_.ErrorDetails.Message
    if(-not $msg){ $msg = (New-Object IO.StreamReader($_.Exception.Response.GetResponseStream())).ReadToEnd() }
    $msg
  }
}

Call Get   /api/users/me 2
Call Post  /api/leaves 2 '{"leaveType":"ANNUAL","startDate":"2026-12-24","endDate":"2026-12-28","reason":"Test"}'
Call Get   /api/admin/leaves/pending 1
Call Patch /api/admin/leaves/1/approve 1 '{"remarks":"Approved"}'
Call Patch /api/admin/leaves/2/reject 1 '{"remarks":"Team deadline"}'
Call Patch /api/leaves/3/cancel 2
```

## 8. Test results

| # | Scenario                                         | Expected | Result |
|---|--------------------------------------------------|----------|--------|
| 1 | Get Bob's profile                                | 200, balance 20 | Pass |
| 2 | Bob applies for leave (5 days)                   | 201, `PENDING`  | Pass |
| 3 | Admin approves the request                       | 200, `APPROVED`, `reviewedById` = 1 | Pass |
| 4 | Balance after approval                           | 15 (20 - 5)     | Pass |
| 5 | Employee calls an admin endpoint                 | 403             | Pass |
| 6 | Overlapping dates with an approved leave         | 400             | Pass |
| 7 | Approve an already approved request              | 400             | Pass |
| 8 | Start date in the past                           | 400             | Pass |
| 9 | End date before start date                       | 400             | Pass |
| 10 | Requested days exceed balance (46 > 15)         | 400             | Pass |
| 11 | Invalid enum value (`HOLIDAY`)                  | 400             | Pass |
| 12 | Missing `X-User-Id` header                      | 400             | Pass |
| 13 | Unknown user / unknown leave id                 | 404             | Pass |
| 14 | Admin rejects with remarks                      | 200, `REJECTED`, remarks saved | Pass |
| 15 | Employee cancels a pending request              | 200, `CANCELLED` | Pass |
| 16 | Balance after reject and cancel                 | Still 15 (only approval deducts) | Pass |

### Screenshots

**1. User profile and leave balance**

![User profile](screenshots/01-user-profile.png)

**2. Approval flow and validation errors (404 / 400)**

![Approve and errors](screenshots/02-approve-and-validation-errors.png)

**3. Balance deducted after approval (15) and overlap / enum / header errors**

![Balance and errors](screenshots/03-balance-after-approval-and-more-errors.png)

**4. Cancel flow; balance unchanged after reject and cancel**

![Cancel and balance](screenshots/04-cancel-and-balance-unchanged.png)

## 9. Project structure

```
src/main/java/com/shadowfox/leavemanagement/
├── controller/   UserController, LeaveController, AdminLeaveController
├── dto/          LeaveApplyRequest, LeaveDecisionRequest, LeaveResponse,
│                 CreateUserRequest, UserResponse, ErrorResponse
├── entity/       User, LeaveRequest, Role, LeaveType, LeaveStatus
├── exception/    BusinessValidationException, ResourceNotFoundException,
│                 ForbiddenException, GlobalExceptionHandler
├── repository/   UserRepository, LeaveRequestRepository
├── service/      UserService, LeaveService
└── LeaveManagementApplication.java
```
