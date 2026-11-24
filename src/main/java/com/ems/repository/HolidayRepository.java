package com.ems.repository;
import com.ems.domain.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
public interface HolidayRepository extends JpaRepository<Holiday, Long> {
    List<Holiday> findAllByOrderByHolidayDateAsc();
    List<Holiday> findByHolidayDateBetween(LocalDate start, LocalDate end);
}
