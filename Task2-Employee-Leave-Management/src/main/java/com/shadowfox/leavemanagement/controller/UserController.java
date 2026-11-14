package com.shadowfox.leavemanagement.controller;

import com.shadowfox.leavemanagement.dto.CreateUserRequest;
import com.shadowfox.leavemanagement.dto.UserResponse;
import com.shadowfox.leavemanagement.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** Admin-only: create a user. */
    @PostMapping
    public ResponseEntity<UserResponse> create(@RequestHeader("X-User-Id") Long adminId,
                                               @Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(adminId, request));
    }

    /** Current user's profile + remaining leave balance. */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(userService.getProfile(userId));
    }
}
