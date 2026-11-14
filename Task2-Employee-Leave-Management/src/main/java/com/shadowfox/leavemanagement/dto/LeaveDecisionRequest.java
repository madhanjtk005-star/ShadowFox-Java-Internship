package com.shadowfox.leavemanagement.dto;

import jakarta.validation.constraints.Size;

/** Optional body for approve / reject calls. */
public record LeaveDecisionRequest(
        @Size(max = 500, message = "remarks must be at most 500 characters")
        String remarks
) {
}
