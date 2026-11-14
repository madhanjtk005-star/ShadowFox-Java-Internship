package com.shadowfox.leavemanagement.dto;

import com.shadowfox.leavemanagement.entity.LeaveRequest;
import com.shadowfox.leavemanagement.entity.LeaveStatus;
import com.shadowfox.leavemanagement.entity.LeaveType;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record LeaveResponse(
        Long id,
        Long employeeId,
        String employeeName,
        LeaveType leaveType,
        LocalDate startDate,
        LocalDate endDate,
        int days,
        String reason,
        LeaveStatus status,
        String adminRemarks,
        Long reviewedById,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt
) {
    public static LeaveResponse from(LeaveRequest l) {
        return new LeaveResponse(
                l.getId(),
                l.getEmployee().getId(),
                l.getEmployee().getName(),
                l.getLeaveType(),
                l.getStartDate(),
                l.getEndDate(),
                l.getDays(),
                l.getReason(),
                l.getStatus(),
                l.getAdminRemarks(),
                l.getReviewedBy() != null ? l.getReviewedBy().getId() : null,
                l.getCreatedAt(),
                l.getReviewedAt());
    }
}
