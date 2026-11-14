package com.shadowfox.leavemanagement.controller;

import com.shadowfox.leavemanagement.dto.LeaveDecisionRequest;
import com.shadowfox.leavemanagement.dto.LeaveResponse;
import com.shadowfox.leavemanagement.entity.LeaveStatus;
import com.shadowfox.leavemanagement.service.LeaveService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/leaves")
public class AdminLeaveController {

    private final LeaveService leaveService;

    public AdminLeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    /** All leave requests; optionally filter with ?status=PENDING|APPROVED|REJECTED|CANCELLED */
    @GetMapping
    public ResponseEntity<List<LeaveResponse>> all(@RequestHeader("X-User-Id") Long adminId,
                                                   @RequestParam(required = false) LeaveStatus status) {
        return ResponseEntity.ok(leaveService.getAll(adminId, status));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<LeaveResponse>> pending(@RequestHeader("X-User-Id") Long adminId) {
        return ResponseEntity.ok(leaveService.getPending(adminId));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<LeaveResponse> approve(@RequestHeader("X-User-Id") Long adminId,
                                                 @PathVariable Long id,
                                                 @Valid @RequestBody(required = false) LeaveDecisionRequest body) {
        return ResponseEntity.ok(leaveService.approve(adminId, id, body == null ? null : body.remarks()));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<LeaveResponse> reject(@RequestHeader("X-User-Id") Long adminId,
                                                @PathVariable Long id,
                                                @Valid @RequestBody(required = false) LeaveDecisionRequest body) {
        return ResponseEntity.ok(leaveService.reject(adminId, id, body == null ? null : body.remarks()));
    }
}
