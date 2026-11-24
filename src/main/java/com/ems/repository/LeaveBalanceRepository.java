package com.ems.repository;
import com.ems.domain.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {
    Optional<LeaveBalance> findByEmployeeIdAndYear(Long employeeId, int year);
}
