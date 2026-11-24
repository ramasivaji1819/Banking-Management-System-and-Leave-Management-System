package com.ems.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public class DepartmentHolidayDtos {
    public record DepartmentRequest(@NotBlank String name, String description) {}
    public record DepartmentResponse(Long id, String name, String description) {}
    public record HolidayRequest(@NotBlank String name, LocalDate holidayDate, boolean recurring) {}
    public record HolidayResponse(Long id, String name, LocalDate holidayDate, boolean recurring) {}
}
