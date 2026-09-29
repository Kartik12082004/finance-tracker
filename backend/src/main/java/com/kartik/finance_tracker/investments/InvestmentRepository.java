package com.kartik.finance_tracker.investments;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InvestmentRepository extends JpaRepository<Investment, UUID> {

    List<Investment> findByUser_Id(UUID userId);

    Optional<Investment> findByIdAndUser_Id(UUID id, UUID userId);
}
