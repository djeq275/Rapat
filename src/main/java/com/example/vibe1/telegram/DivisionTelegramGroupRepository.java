package com.example.vibe1.telegram;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DivisionTelegramGroupRepository extends JpaRepository<DivisionTelegramGroup, Long> {

    @Query("select d from DivisionTelegramGroup d join fetch d.telegramGroup where d.division.id = :divisionId")
    List<DivisionTelegramGroup> findByDivisionId(Long divisionId);

    boolean existsByDivisionIdAndTelegramGroupId(Long divisionId, Long telegramGroupId);

    void deleteByDivisionIdAndTelegramGroupId(Long divisionId, Long telegramGroupId);
}
