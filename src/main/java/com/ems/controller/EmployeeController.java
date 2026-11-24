package com.ems.controller;

import com.ems.domain.EmploymentStatus;
import com.ems.dto.EmployeeDtos.*;
import com.ems.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {
    private final EmployeeService service;

    public EmployeeController(EmployeeService service) {
        this.service = service;
    }

    @GetMapping("/me")
    public EmployeeResponse me() {
        return service.getOwnProfile();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public Page<EmployeeResponse> list(Pageable pageable) {
        return service.list(pageable);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeResponse create(@Valid @RequestBody EmployeeRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public EmployeeResponse update(@PathVariable Long id, @Valid @RequestBody EmployeeRequest request) {
        return service.update(id, request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public EmployeeResponse get(@PathVariable Long id) {
        return service.getById(id);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public EmployeeResponse status(@PathVariable Long id, @RequestParam EmploymentStatus status) {
        return service.updateStatus(id, status);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.softDelete(id);
    }

    @GetMapping("/{id}/subordinates")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public List<EmployeeResponse> subordinates(@PathVariable Long id) {
        return service.subordinates(id);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public Page<EmployeeResponse> search(@RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) EmploymentStatus status,
                                         @RequestParam(required = false) Long departmentId,
                                         Pageable pageable) {
        return service.search(keyword, status, departmentId, pageable);
    }
}
