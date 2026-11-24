package com.ems.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "leave_balances", uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "year"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LeaveBalance {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id")
    private Employee employee;
    @Column(name = "year", nullable = false)
    private int year;
    @Builder.Default private double annualEntitlement = 20;
    @Builder.Default private double sickEntitlement = 10;
    @Builder.Default private double annualUsed = 0;
    @Builder.Default private double sickUsed = 0;
    @Builder.Default private double personalUsed = 0;
    @Builder.Default private double unpaidUsed = 0;
}
