package com.example.portfolio_service.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "portfolios",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"userId", "companyId", "portfolioGroupId"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔹 User reference (from auth-service)
    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long portfolioGroupId;

    // 🔹 Company reference (from company-service)
    @Column(nullable = false)
    private String companyId;

    // 🔹 Total shares owned
    @Column(nullable = false)
    private int quantity;

    // 🔹 Weighted average buy price
    @Column(nullable = false)
    private double averageBuyPrice;

    private Double stopLossPrice;

    // 🔹 Audit fields
    @UpdateTimestamp
    private LocalDateTime lastUpdated;
}
