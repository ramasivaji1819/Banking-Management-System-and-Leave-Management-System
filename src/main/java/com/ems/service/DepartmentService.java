package com.ems.service;

import com.ems.domain.Department;
import com.ems.dto.DepartmentHolidayDtos.*;
import com.ems.dto.EmployeeDtos.EmployeeResponse;
import com.ems.exception.ApiException;
import com.ems.repository.DepartmentRepository;
import com.ems.repository.EmployeeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DepartmentService {
    private final DepartmentRepository departments;
    private final EmployeeRepository employees;

    public DepartmentService(DepartmentRepository departments, EmployeeRepository employees) {
        this.departments = departments;
        this.employees = employees;
    }

    public List<DepartmentResponse> list() {
        return departments.findByDeletedFalse().stream().map(this::toResponse).toList();
    }

    public DepartmentResponse create(DepartmentRequest request) {
        Department department = Department.builder().name(request.name()).description(request.description()).build();
        return toResponse(departments.save(department));
    }

    public DepartmentResponse update(Long id, DepartmentRequest request) {
        Department department = findActive(id);
        department.setName(request.name());
        department.setDescription(request.description());
        return toResponse(departments.save(department));
    }

    public void delete(Long id) {
        Department department = findActive(id);
        department.setDeleted(true);
        departments.save(department);
    }

    public List<EmployeeResponse> employees(Long id) {
        findActive(id);
        return employees.findAll().stream()
                .filter(e -> !e.isDeleted() && e.getDepartment() != null && e.getDepartment().getId().equals(id))
                .map(e -> new EmployeeResponse(e.getId(), e.getEmployeeCode(), e.getFirstName(), e.getLastName(),
                        e.getEmail(), e.getPhone(), e.getPosition(), e.getHireDate(), e.getStatus(),
                        id, e.getDepartment().getName(),
                        e.getManager() == null ? null : e.getManager().getId(),
                        e.getManager() == null ? null : e.getManager().getFirstName() + " " + e.getManager().getLastName()))
                .toList();
    }

    private Department findActive(Long id) {
        return departments.findById(id).filter(d -> !d.isDeleted())
                .orElseThrow(() -> new ApiException("Department not found", HttpStatus.NOT_FOUND));
    }

    private DepartmentResponse toResponse(Department d) {
        return new DepartmentResponse(d.getId(), d.getName(), d.getDescription());
    }
}
