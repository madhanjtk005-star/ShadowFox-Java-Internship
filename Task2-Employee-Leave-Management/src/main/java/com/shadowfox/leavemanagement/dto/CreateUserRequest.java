package com.shadowfox.leavemanagement.dto;

import com.shadowfox.leavemanagement.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateUserRequest(
        @NotBlank(message = "name is required") String name,
        @NotBlank(message = "email is required") @Email(message = "email must be valid") String email,
        @NotNull(message = "role is required") Role role,
        @Min(value = 0, message = "leaveBalance cannot be negative") int leaveBalance
) {
}
