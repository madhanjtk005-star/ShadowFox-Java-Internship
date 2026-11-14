package com.shadowfox.leavemanagement.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.shadowfox.leavemanagement.entity.LeaveType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record LeaveApplyRequest(
        @NotNull(message = "leaveType is required")
        LeaveType leaveType,

        @NotNull(message = "startDate is required")
        @FutureOrPresent(message = "startDate cannot be in the past")
        LocalDate startDate,

        @NotNull(message = "endDate is required")
        LocalDate endDate,

        @NotBlank(message = "reason is required")
        @Size(max = 500, message = "reason must be at most 500 characters")
        String reason
) {
    @JsonIgnore
    @AssertTrue(message = "endDate must be on or after startDate")
    public boolean isEndDateValid() {
        return startDate == null || endDate == null || !endDate.isBefore(startDate);
    }
}
