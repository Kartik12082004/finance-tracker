package com.kartik.finance_tracker.goals;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalRepository extends JpaRepository<Goal, UUID> {

    List<Goal> findByUser_Id(UUID userId);

    Optional<Goal> findByIdAndUser_Id(UUID id, UUID userId);
}
