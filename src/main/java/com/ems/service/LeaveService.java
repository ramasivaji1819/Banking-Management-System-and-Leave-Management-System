package com.ems.service;

import com.ems.domain.*;
import com.ems.dto.LeaveDtos.*;
import com.ems.exception.ApiException;
import com.ems.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class LeaveService {
    private final LeaveRequestRepository leaves;
    private final LeaveBalanceRepository balances;
    private final EmployeeService employees;
    private final AuthService authService;

    public LeaveService(LeaveRequestRepository leaves, LeaveBalanceRepository balances,
                        EmployeeService employees, AuthService authService) {
        this.leaves = leaves;
        this.balances = balances;
        this.employees = employees;
        this.authService = authService;
    }

    public Page<LeaveResponse> myRequests(Pageable pageable) {
        Employee employee = employees.findByCurrentUser();
        return leaves.findByEmployeeId(employee.getId(), pageable).map(this::toResponse);
    }

    public Page<LeaveResponse> all(Pageable pageable) {
        return leaves.findAll(pageable).map(this::toResponse);
    }

    public Page<LeaveResponse> pending(Pageable pageable) {
        return leaves.findByStatus(LeaveStatus.PENDING, pageable).map(this::toResponse);
    }

    @Transactional
    public LeaveResponse apply(LeaveRequestDto request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new ApiException("End date must not be before start date", HttpStatus.BAD_REQUEST);
        }
        if (request.startDate().isBefore(LocalDate.now())) {
            throw new ApiException("Leave cannot start in the past", HttpStatus.BAD_REQUEST);
        }
        LeaveRequest leave = LeaveRequest.builder()
                .employee(employees.findByCurrentUser()).leaveType(request.leaveType())
                .startDate(request.startDate()).endDate(request.endDate()).reason(request.reason())
                .status(LeaveStatus.PENDING).build();
        return toResponse(leaves.save(leave));
    }

    @Transactional
    public LeaveResponse approve(Long id) {
        LeaveRequest leave = findPending(id);
        double days = workingDays(leave);
        LeaveBalance balance = getBalance(leave.getEmployee(), leave.getStartDate().getYear());
        deduct(balance, leave.getLeaveType(), days);
        leave.setStatus(LeaveStatus.APPROVED);
        leave.setApprovedBy(employees.findByCurrentUser());
        leave.setDecidedAt(LocalDateTime.now());
        return toResponse(leaves.save(leave));
    }

    @Transactional
    public LeaveResponse reject(Long id, RejectRequest request) {
        LeaveRequest leave = findPending(id);
        leave.setStatus(LeaveStatus.REJECTED);
        leave.setRejectionReason(request.reason());
        leave.setApprovedBy(employees.findByCurrentUser());
        leave.setDecidedAt(LocalDateTime.now());
        return toResponse(leaves.save(leave));
    }

    @Transactional
    public LeaveResponse update(Long id, LeaveRequestDto request) {
        LeaveRequest leave = findOwned(id);
        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new ApiException("Only pending requests can be updated", HttpStatus.BAD_REQUEST);
        }
        leave.setLeaveType(request.leaveType());
        leave.setStartDate(request.startDate());
        leave.setEndDate(request.endDate());
        leave.setReason(request.reason());
        return toResponse(leaves.save(leave));
    }

    @Transactional
    public LeaveResponse cancel(Long id) {
        LeaveRequest leave = leaves.findById(id)
                .orElseThrow(() -> new ApiException("Leave request not found", HttpStatus.NOT_FOUND));
        Employee current = employees.findByCurrentUser();
        boolean owner = leave.getEmployee().getId().equals(current.getId());
        boolean privileged = authService.currentUser().getRole() != Role.EMPLOYEE;
        if (!owner && !privileged) throw new ApiException("Forbidden", HttpStatus.FORBIDDEN);
        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new ApiException("Only pending requests can be cancelled", HttpStatus.BAD_REQUEST);
        }
        leave.setStatus(LeaveStatus.CANCELLED);
        return toResponse(leaves.save(leave));
    }

    public LeaveResponse getById(Long id) {
        LeaveRequest leave = leaves.findById(id)
                .orElseThrow(() -> new ApiException("Leave request not found", HttpStatus.NOT_FOUND));
        Employee current = employees.findByCurrentUser();
        User user = authService.currentUser();
        boolean owner = leave.getEmployee().getId().equals(current.getId());
        if (!owner && user.getRole() == Role.EMPLOYEE) throw new ApiException("Forbidden", HttpStatus.FORBIDDEN);
        return toResponse(leave);
    }

    public LeaveBalanceResponse balance() {
        Employee employee = employees.findByCurrentUser();
        return toBalance(getBalance(employee, LocalDate.now().getYear()));
    }

    public LeaveBalanceResponse balanceForEmployee(Long employeeId) {
        Employee employee = employees.findActive(employeeId);
        return toBalance(getBalance(employee, LocalDate.now().getYear()));
    }

    @Transactional
    public LeaveBalanceResponse initialize(int year) {
        Employee employee = employees.findByCurrentUser();
        return toBalance(getBalance(employee, year));
    }

    public LeaveBalance getBalance(Employee employee, int year) {
        return balances.findByEmployeeIdAndYear(employee.getId(), year).orElseGet(() ->
                balances.save(LeaveBalance.builder().employee(employee).year(year).build()));
    }

    private LeaveRequest findPending(Long id) {
        LeaveRequest leave = leaves.findById(id)
                .orElseThrow(() -> new ApiException("Leave request not found", HttpStatus.NOT_FOUND));
        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new ApiException("Leave request is not pending", HttpStatus.BAD_REQUEST);
        }
        return leave;
    }

    private LeaveRequest findOwned(Long id) {
        LeaveRequest leave = leaves.findById(id)
                .orElseThrow(() -> new ApiException("Leave request not found", HttpStatus.NOT_FOUND));
        if (!leave.getEmployee().getId().equals(employees.findByCurrentUser().getId())) {
            throw new ApiException("Forbidden", HttpStatus.FORBIDDEN);
        }
        return leave;
    }

    private double workingDays(LeaveRequest leave) {
        double days = 0;
        for (LocalDate date = leave.getStartDate(); !date.isAfter(leave.getEndDate()); date = date.plusDays(1)) {
            if (date.getDayOfWeek().getValue() < 6) days++;
        }
        return days;
    }

    private void deduct(LeaveBalance balance, LeaveType type, double days) {
        switch (type) {
            case ANNUAL -> {
                if (balance.getAnnualUsed() + days > balance.getAnnualEntitlement())
                    throw new ApiException("Insufficient annual leave balance", HttpStatus.BAD_REQUEST);
                balance.setAnnualUsed(balance.getAnnualUsed() + days);
            }
            case SICK -> {
                if (balance.getSickUsed() + days > balance.getSickEntitlement())
                    throw new ApiException("Insufficient sick leave balance", HttpStatus.BAD_REQUEST);
                balance.setSickUsed(balance.getSickUsed() + days);
            }
            case PERSONAL -> balance.setPersonalUsed(balance.getPersonalUsed() + days);
            case UNPAID -> balance.setUnpaidUsed(balance.getUnpaidUsed() + days);
            default -> balance.setAnnualUsed(balance.getAnnualUsed() + days);
        }
        balances.save(balance);
    }

    private LeaveBalanceResponse toBalance(LeaveBalance b) {
        return new LeaveBalanceResponse(b.getYear(), b.getAnnualEntitlement(), b.getSickEntitlement(),
                b.getAnnualUsed(), b.getSickUsed(), b.getPersonalUsed(), b.getUnpaidUsed());
    }

    private LeaveResponse toResponse(LeaveRequest l) {
        Employee e = l.getEmployee();
        return new LeaveResponse(l.getId(), e.getId(), e.getFirstName() + " " + e.getLastName(),
                l.getLeaveType(), l.getStartDate(), l.getEndDate(), l.getReason(),
                l.getStatus(), l.getRejectionReason());
    }
}
