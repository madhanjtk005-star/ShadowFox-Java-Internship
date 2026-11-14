package com.shadowfox.leavemanagement.repository;

import com.shadowfox.leavemanagement.entity.LeaveRequest;
import com.shadowfox.leavemanagement.entity.LeaveStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);

    List<LeaveRequest> findByStatusOrderByCreatedAtAsc(LeaveStatus status);

    List<LeaveRequest> findAllByOrderByCreatedAtDesc();

    /** True when [start, end] intersects any existing leave of the employee in the given statuses. */
    @Query("""
            select count(l) > 0 from LeaveRequest l
            where l.employee.id = :employeeId
              and l.status in :statuses
              and l.startDate <= :endDate
              and l.endDate >= :startDate
            """)
    boolean existsOverlapping(@Param("employeeId") Long employeeId,
                              @Param("startDate") LocalDate startDate,
                              @Param("endDate") LocalDate endDate,
                              @Param("statuses") Collection<LeaveStatus> statuses);
}
