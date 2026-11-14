package com.shadowfox.leavemanagement.dto;

import com.shadowfox.leavemanagement.entity.Role;
import com.shadowfox.leavemanagement.entity.User;

public record UserResponse(Long id, String name, String email, Role role, int leaveBalance) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.getLeaveBalance());
    }
}
