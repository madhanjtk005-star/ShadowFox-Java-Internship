package com.shadowfox.leavemanagement.controller;

import com.shadowfox.leavemanagement.dto.LeaveApplyRequest;
import com.shadowfox.leavemanagement.dto.LeaveResponse;
import com.shadowfox.leavemanagement.service.LeaveService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/leaves")
public class LeaveController {

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @PostMapping
    public ResponseEntity<LeaveResponse> apply(@RequestHeader("X-User-Id") Long userId,
                                               @Valid @RequestBody LeaveApplyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leaveService.apply(userId, request));
    }

    @GetMapping("/my")
    public ResponseEntity<List<LeaveResponse>> myLeaves(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(leaveService.getMyLeaves(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeaveResponse> getOne(@RequestHeader("X-User-Id") Long userId,
                                                @PathVariable Long id) {
        return ResponseEntity.ok(leaveService.getLeave(userId, id));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<LeaveResponse> cancel(@RequestHeader("X-User-Id") Long userId,
                                                @PathVariable Long id) {
        return ResponseEntity.ok(leaveService.cancel(userId, id));
    }
}
