package com.ems.controller;

import com.ems.dto.LeaveDtos.*;
import com.ems.service.LeaveService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/leaves")
public class LeaveController {
    private final LeaveService service;

    public LeaveController(LeaveService service) {
        this.service = service;
    }

    @GetMapping("/my-requests")
    public Page<LeaveResponse> myRequests(Pageable pageable) {
        return service.myRequests(pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LeaveResponse apply(@Valid @RequestBody LeaveRequestDto request) {
        return service.apply(request);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('MANAGER','HR','ADMIN')")
    public Page<LeaveResponse> pending(Pageable pageable) {
        return service.pending(pageable);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER','HR','ADMIN')")
    public LeaveResponse approve(@PathVariable Long id) {
        return service.approve(id);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('MANAGER','HR','ADMIN')")
    public LeaveResponse reject(@PathVariable Long id, @RequestBody(required = false) RejectRequest request) {
        return service.reject(id, request == null ? new RejectRequest("Rejected") : request);
    }

    @GetMapping("/balance")
    public LeaveBalanceResponse balance() {
        return service.balance();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public Page<LeaveResponse> all(Pageable pageable) {
        return service.all(pageable);
    }

    @GetMapping("/{id}")
    public LeaveResponse get(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public LeaveResponse update(@PathVariable Long id, @Valid @RequestBody LeaveRequestDto request) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/cancel")
    public LeaveResponse cancel(@PathVariable Long id) {
        return service.cancel(id);
    }

    @GetMapping("/balance/{empId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public LeaveBalanceResponse balanceForEmployee(@PathVariable Long empId) {
        return service.balanceForEmployee(empId);
    }

    @PostMapping("/balance/initialize")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public LeaveBalanceResponse initialize(@RequestParam(required = false) Integer year) {
        return service.initialize(year == null ? java.time.LocalDate.now().getYear() : year);
    }

    @GetMapping("/calendar")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public Page<LeaveResponse> calendar(Pageable pageable) {
        return service.all(pageable);
    }
}
