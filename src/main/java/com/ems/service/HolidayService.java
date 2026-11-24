package com.ems.service;

import com.ems.domain.Holiday;
import com.ems.dto.DepartmentHolidayDtos.*;
import com.ems.exception.ApiException;
import com.ems.repository.HolidayRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class HolidayService {
    private final HolidayRepository holidays;

    public HolidayService(HolidayRepository holidays) {
        this.holidays = holidays;
    }

    public List<HolidayResponse> list() {
        return holidays.findAllByOrderByHolidayDateAsc().stream().map(this::toResponse).toList();
    }

    public HolidayResponse create(HolidayRequest request) {
        return toResponse(holidays.save(Holiday.builder()
                .name(request.name()).holidayDate(request.holidayDate()).recurring(request.recurring()).build()));
    }

    public HolidayResponse update(Long id, HolidayRequest request) {
        Holiday holiday = find(id);
        holiday.setName(request.name());
        holiday.setHolidayDate(request.holidayDate());
        holiday.setRecurring(request.recurring());
        return toResponse(holidays.save(holiday));
    }

    public void delete(Long id) {
        holidays.delete(find(id));
    }

    private Holiday find(Long id) {
        return holidays.findById(id).orElseThrow(() ->
                new ApiException("Holiday not found", HttpStatus.NOT_FOUND));
    }

    private HolidayResponse toResponse(Holiday h) {
        return new HolidayResponse(h.getId(), h.getName(), h.getHolidayDate(), h.isRecurring());
    }
}
