package com.ems.dto;

import com.ems.domain.LeaveStatus;
import com.ems.domain.LeaveType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class LeaveDtos {
    public record LeaveRequestDto(@NotNull LeaveType leaveType, @NotNull LocalDate startDate,
                                  @NotNull LocalDate endDate, String reason) {}
    public record RejectRequest(String reason) {}
    public record LeaveResponse(Long id, Long employeeId, String employeeName, LeaveType leaveType,
                                LocalDate startDate, LocalDate endDate, String reason, LeaveStatus status,
                                String rejectionReason) {}
    public record LeaveBalanceResponse(int year, double annualEntitlement, double sickEntitlement,
                                       double annualUsed, double sickUsed, double personalUsed,
                                       double unpaidUsed) {}
}
