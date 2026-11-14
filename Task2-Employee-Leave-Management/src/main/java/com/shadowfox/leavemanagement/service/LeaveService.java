package com.shadowfox.leavemanagement.service;

import com.shadowfox.leavemanagement.dto.LeaveApplyRequest;
import com.shadowfox.leavemanagement.dto.LeaveResponse;
import com.shadowfox.leavemanagement.entity.LeaveRequest;
import com.shadowfox.leavemanagement.entity.LeaveStatus;
import com.shadowfox.leavemanagement.entity.Role;
import com.shadowfox.leavemanagement.entity.User;
import com.shadowfox.leavemanagement.exception.BusinessValidationException;
import com.shadowfox.leavemanagement.exception.ForbiddenException;
import com.shadowfox.leavemanagement.exception.ResourceNotFoundException;
import com.shadowfox.leavemanagement.repository.LeaveRequestRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeaveService {

    private static final Set<LeaveStatus> BLOCKING_STATUSES = Set.of(LeaveStatus.PENDING, LeaveStatus.APPROVED);

    private final LeaveRequestRepository leaveRepository;
    private final UserService userService;

    public LeaveService(LeaveRequestRepository leaveRepository, UserService userService) {
        this.leaveRepository = leaveRepository;
        this.userService = userService;
    }

    // ---------------------------------------------------------------- employee

    @Transactional
    public LeaveResponse apply(Long userId, LeaveApplyRequest req) {
        User employee = userService.getById(userId);

        validateDates(req.startDate(), req.endDate());
        int days = countDays(req.startDate(), req.endDate());

        if (leaveRepository.existsOverlapping(userId, req.startDate(), req.endDate(), BLOCKING_STATUSES)) {
            throw new BusinessValidationException(
                    "Leave dates overlap with an existing PENDING or APPROVED leave request");
        }
        if (employee.getLeaveBalance() < days) {
            throw new BusinessValidationException("Insufficient leave balance: requested " + days
                    + " day(s), available " + employee.getLeaveBalance());
        }

        LeaveRequest leave = new LeaveRequest();
        leave.setEmployee(employee);
        leave.setLeaveType(req.leaveType());
        leave.setStartDate(req.startDate());
        leave.setEndDate(req.endDate());
        leave.setDays(days);
        leave.setReason(req.reason().trim());
        leave.setStatus(LeaveStatus.PENDING);

        return LeaveResponse.from(leaveRepository.save(leave));
    }

    @Transactional(readOnly = true)
    public List<LeaveResponse> getMyLeaves(Long userId) {
        userService.getById(userId);
        return leaveRepository.findByEmployeeIdOrderByCreatedAtDesc(userId)
                .stream().map(LeaveResponse::from).toList();
    }

    /** Employees can see only their own request; admins can see any. */
    @Transactional(readOnly = true)
    public LeaveResponse getLeave(Long userId, Long leaveId) {
        User caller = userService.getById(userId);
        LeaveRequest leave = findLeave(leaveId);
        if (caller.getRole() != Role.ADMIN && !leave.getEmployee().getId().equals(userId)) {
            throw new ForbiddenException("You can only view your own leave requests");
        }
        return LeaveResponse.from(leave);
    }

    @Transactional
    public LeaveResponse cancel(Long userId, Long leaveId) {
        userService.getById(userId);
        LeaveRequest leave = findLeave(leaveId);
        if (!leave.getEmployee().getId().equals(userId)) {
            throw new ForbiddenException("You can only cancel your own leave requests");
        }
        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new BusinessValidationException(
                    "Only PENDING requests can be cancelled (current status: " + leave.getStatus() + ")");
        }
        leave.setStatus(LeaveStatus.CANCELLED); // balance was never deducted, so nothing to refund
        return LeaveResponse.from(leave);
    }

    // ------------------------------------------------------------------- admin

    @Transactional(readOnly = true)
    public List<LeaveResponse> getAll(Long adminId, LeaveStatus status) {
        userService.requireAdmin(adminId);
        List<LeaveRequest> list = (status == null)
                ? leaveRepository.findAllByOrderByCreatedAtDesc()
                : leaveRepository.findByStatusOrderByCreatedAtAsc(status);
        return list.stream().map(LeaveResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<LeaveResponse> getPending(Long adminId) {
        return getAll(adminId, LeaveStatus.PENDING);
    }

    @Transactional
    public LeaveResponse approve(Long adminId, Long leaveId, String remarks) {
        User admin = userService.requireAdmin(adminId);
        LeaveRequest leave = findPending(leaveId);

        User employee = leave.getEmployee();
        if (employee.getLeaveBalance() < leave.getDays()) {
            throw new BusinessValidationException("Cannot approve: employee has " + employee.getLeaveBalance()
                    + " day(s) left but the request needs " + leave.getDays());
        }
        employee.setLeaveBalance(employee.getLeaveBalance() - leave.getDays()); // deduct only on approval

        return LeaveResponse.from(finalizeRequest(leave, LeaveStatus.APPROVED, admin, remarks));
    }

    @Transactional
    public LeaveResponse reject(Long adminId, Long leaveId, String remarks) {
        User admin = userService.requireAdmin(adminId);
        LeaveRequest leave = findPending(leaveId);
        return LeaveResponse.from(finalizeRequest(leave, LeaveStatus.REJECTED, admin, remarks));
    }

    // ----------------------------------------------------------------- helpers

    private LeaveRequest finalizeRequest(LeaveRequest leave, LeaveStatus status, User admin, String remarks) {
        leave.setStatus(status);
        leave.setReviewedBy(admin);
        leave.setReviewedAt(LocalDateTime.now());
        leave.setAdminRemarks(remarks == null || remarks.isBlank() ? null : remarks.trim());
        return leave;
    }

    private LeaveRequest findLeave(Long id) {
        return leaveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with id " + id));
    }

    private LeaveRequest findPending(Long id) {
        LeaveRequest leave = findLeave(id);
        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new BusinessValidationException(
                    "Only PENDING requests can be approved or rejected (current status: " + leave.getStatus() + ")");
        }
        return leave;
    }

    private void validateDates(LocalDate start, LocalDate end) {
        if (start.isBefore(LocalDate.now())) {
            throw new BusinessValidationException("startDate cannot be in the past");
        }
        if (end.isBefore(start)) {
            throw new BusinessValidationException("endDate must be on or after startDate");
        }
    }

    /** Inclusive calendar days (weekends/holidays are counted). */
    private int countDays(LocalDate start, LocalDate end) {
        return (int) ChronoUnit.DAYS.between(start, end) + 1;
    }
}
