# Student Helpdesk – Support Ticket Console System

A console-based Java application where students log support issues and support agents work them through to resolution. Built with OOP principles, enums, collections and a clear separation between model, business logic, input handling and UI.

## Features

- **Create tickets** with title, description, category and priority (IDs auto-generated: `TKT-1001`, `TKT-1002`, ...)
- **Update tickets**: title, description, category, priority (blank = keep current value)
- **Assign tickets** to a support agent (agents only)
- **Status workflow** with enforced transitions: `OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED`; a ticket that is `RESOLVED` or `CLOSED` can be **reopened** back to `OPEN`
- **View / search**: all tickets, details by ID, filter by status, category, priority, keyword, or "my tickets"
- **Summary** of ticket counts per status
- **Roles**: students create/update/reopen their own tickets; agents can additionally assign, start work, resolve and close

## Validations handled

| Case | Behaviour |
|------|-----------|
| Blank / whitespace-only title, description, resolution note | Rejected, user is re-prompted (or service throws `IllegalArgumentException`) |
| Non-numeric or out-of-range menu input | Message shown, re-prompt, no crash |
| Duplicate ticket IDs | Impossible: IDs come from a sequential counter |
| Illogical status jumps (e.g. `OPEN -> CLOSED`) | Rejected by `TicketStatus.canTransitionTo` |
| Updating a `CLOSED` ticket | Blocked until it is reopened |
| Unknown ticket ID | Clear error message |
| Student attempting agent-only actions, or editing someone else's ticket | Blocked |
| End of input stream (Ctrl+D / Ctrl+Z) | Exits cleanly |

Input is read with `nextLine()` and parsed manually, which avoids `InputMismatchException` and leftover-newline bugs entirely.

## Project structure

```
StudentHelpdeskSystem/
├── src/
│   ├── model/
│   │   ├── Ticket.java
│   │   ├── User.java            (contains Role enum: STUDENT, SUPPORT_AGENT)
│   │   ├── TicketStatus.java    (OPEN, IN_PROGRESS, RESOLVED, CLOSED + transition rules)
│   │   ├── Priority.java        (LOW, MEDIUM, HIGH, URGENT)
│   │   └── Category.java        (IT_SUPPORT, ACADEMICS, FACILITIES, OTHER)
│   ├── service/
│   │   └── HelpdeskService.java (create, update, assign, transitions, search; HashMap/LinkedHashMap + List)
│   ├── util/
│   │   └── InputValidator.java  (safe int / non-empty string / enum / confirm input)
│   └── Main.java                (menu loop and console UI only)
└── README.md
```

## Setup and run

Requires JDK 11 or newer.

```bash
cd StudentHelpdeskSystem
mkdir out
javac -d out $(find src -name "*.java")      # Linux / macOS
java -cp out Main
```

On Windows (PowerShell / cmd):

```bat
mkdir out
dir /s /b src\*.java > sources.txt
javac -d out @sources.txt
java -cp out Main
```

Or open the folder in IntelliJ IDEA / Eclipse / VS Code, mark `src` as the sources root, and run `Main`.

Pre-loaded users: `Asha` and `Ravi` (students), `Meena` and `Karthik` (support agents). Choose one at startup; use menu option 12 to switch.

## Sample output

```
Select a user to log in as:
  1. Asha (S101, Student)
  ...
Choose (1-4): 1
Logged in as Asha (S101, Student)

Enter choice: 1
--- Create Ticket ---
Title:
  ! This field cannot be empty.
Title: Wi-Fi not working
Description: Cannot connect in library
Category:
  1. IT Support
  2. Academics
  3. Facilities / Hostel
  4. Other
Choose (1-4): abc
  ! Invalid input. Please enter a number.
Choose (1-4): 9
  ! Please enter a number between 1 and 4.
Choose (1-4): 1
Priority: ... Choose (1-4): 2
  [OK] Ticket created with ID TKT-1001

Enter choice: 2
ID        | Title                        | Category            | Priority | Status      | Assigned To
----------------------------------------------------------------------------------------------------
TKT-1001  | Wi-Fi not working            | IT Support          | Medium   | Open        | Unassigned
1 ticket(s).

Enter choice: 7            <- as a student
Ticket ID: TKT-1001
  [ERROR] Only support agents can perform this action.

(switch to agent Meena)
Enter choice: 9            <- close an OPEN ticket
  [ERROR] Invalid status change for TKT-1001: Open -> Closed is not allowed.

Enter choice: 7
  [OK] TKT-1001 is now In Progress
```

## Possible extensions

- Persist tickets to a file (CSV / JSON) so data survives restarts
- Comments / activity history per ticket
- Unit tests for `HelpdeskService` and `TicketStatus`
