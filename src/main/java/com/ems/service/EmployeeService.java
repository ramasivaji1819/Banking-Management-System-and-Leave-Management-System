package com.ems.service;

import com.ems.domain.*;
import com.ems.dto.EmployeeDtos.*;
import com.ems.exception.ApiException;
import com.ems.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
public class EmployeeService {
    private final EmployeeRepository employees;
    private final DepartmentRepository departments;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    public EmployeeService(EmployeeRepository employees, DepartmentRepository departments,
                           UserRepository users, PasswordEncoder passwordEncoder, AuthService authService) {
        this.employees = employees;
        this.departments = departments;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
    }

    public EmployeeResponse getOwnProfile() {
        Employee employee = findByCurrentUser();
        return toResponse(employee);
    }

    public Page<EmployeeResponse> list(Pageable pageable) {
        return employees.findAll(pageable).map(this::toResponse);
    }

    public EmployeeResponse getById(Long id) {
        return toResponse(findActive(id));
    }

    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        if (employees.existsByEmployeeCode(request.employeeCode()) ||
                users.existsByEmail(request.email()) ||
                (request.username() != null && users.existsByUsername(request.username()))) {
            throw new ApiException("Employee code, username, or email already exists", HttpStatus.CONFLICT);
        }
        String username = request.username() == null
                ? request.email().substring(0, request.email().indexOf('@')) : request.username();
        if (users.existsByUsername(username)) {
            throw new ApiException("Username already exists", HttpStatus.CONFLICT);
        }
        Department department = request.departmentId() == null ? null :
                departments.findById(request.departmentId())
                        .orElseThrow(() -> new ApiException("Department not found", HttpStatus.NOT_FOUND));
        Employee manager = request.managerId() == null ? null : findActive(request.managerId());
        User user = User.builder()
                .username(username).email(request.email())
                .password(passwordEncoder.encode(request.password() == null ? "Password123!" : request.password()))
                .role(Role.EMPLOYEE).enabled(true).build();
        Employee employee = Employee.builder()
                .employeeCode(request.employeeCode()).firstName(request.firstName()).lastName(request.lastName())
                .email(request.email()).phone(request.phone()).position(request.position())
                .hireDate(request.hireDate() == null ? LocalDate.now() : request.hireDate())
                .status(EmploymentStatus.ACTIVE).department(department).manager(manager).user(user).build();
        return toResponse(employees.save(employee));
    }

    @Transactional
    public EmployeeResponse update(Long id, EmployeeRequest request) {
        Employee employee = findActive(id);
        employee.setFirstName(request.firstName());
        employee.setLastName(request.lastName());
        employee.setPhone(request.phone());
        employee.setPosition(request.position());
        if (request.departmentId() != null) {
            employee.setDepartment(departments.findById(request.departmentId())
                    .orElseThrow(() -> new ApiException("Department not found", HttpStatus.NOT_FOUND)));
        }
        if (request.managerId() != null) {
            employee.setManager(findActive(request.managerId()));
        }
        return toResponse(employees.save(employee));
    }

    public EmployeeResponse updateStatus(Long id, EmploymentStatus status) {
        Employee employee = findActive(id);
        employee.setStatus(status);
        return toResponse(employees.save(employee));
    }

    public void softDelete(Long id) {
        Employee employee = findActive(id);
        employee.setDeleted(true);
        employee.getUser().setEnabled(false);
        employees.save(employee);
    }

    public List<EmployeeResponse> subordinates(Long id) {
        findActive(id);
        return employees.findByManagerIdAndDeletedFalse(id).stream().map(this::toResponse).toList();
    }

    public Page<EmployeeResponse> search(String keyword, EmploymentStatus status, Long departmentId,
                                         Pageable pageable) {
        Page<Employee> result = employees.findAll(pageable);
        if (keyword == null && status == null && departmentId == null) return result.map(this::toResponse);
        List<Employee> filtered = result.getContent().stream()
                .filter(e -> !e.isDeleted())
                .filter(e -> keyword == null || (e.getFirstName() + " " + e.getLastName() + " " + e.getEmail())
                        .toLowerCase().contains(keyword.toLowerCase()))
                .filter(e -> status == null || e.getStatus() == status)
                .filter(e -> departmentId == null || e.getDepartment() != null && e.getDepartment().getId().equals(departmentId))
                .toList();
        return new org.springframework.data.domain.PageImpl<>(filtered.stream().map(this::toResponse).toList(),
                pageable, filtered.size());
    }

    public Employee findByCurrentUser() {
        String username = authService.currentUser().getUsername();
        return employees.findByUserUsername(username)
                .orElseThrow(() -> new ApiException("Employee profile not found", HttpStatus.NOT_FOUND));
    }

    public Employee findActive(Long id) {
        return employees.findById(id).filter(e -> !e.isDeleted())
                .orElseThrow(() -> new ApiException("Employee not found", HttpStatus.NOT_FOUND));
    }

    private EmployeeResponse toResponse(Employee e) {
        return new EmployeeResponse(e.getId(), e.getEmployeeCode(), e.getFirstName(), e.getLastName(),
                e.getEmail(), e.getPhone(), e.getPosition(), e.getHireDate(), e.getStatus(),
                e.getDepartment() == null ? null : e.getDepartment().getId(),
                e.getDepartment() == null ? null : e.getDepartment().getName(),
                e.getManager() == null ? null : e.getManager().getId(),
                e.getManager() == null ? null : e.getManager().getFirstName() + " " + e.getManager().getLastName());
    }
}
