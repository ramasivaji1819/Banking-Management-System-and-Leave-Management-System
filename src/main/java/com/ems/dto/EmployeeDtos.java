package com.ems.dto;

import com.ems.domain.EmploymentStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public class EmployeeDtos {
    public record EmployeeRequest(
            @NotBlank String employeeCode, @NotBlank String firstName, @NotBlank String lastName,
            @NotBlank @Email String email, String phone, String position, LocalDate hireDate,
            Long departmentId, Long managerId, String username, String password) {}
    public record EmployeeResponse(Long id, String employeeCode, String firstName, String lastName,
                                   String email, String phone, String position, LocalDate hireDate,
                                   EmploymentStatus status, Long departmentId, String departmentName,
                                   Long managerId, String managerName) {}
}
