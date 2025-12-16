package com.example.profile.repository;

import com.example.profile.model.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SkillRepository extends JpaRepository<Skill, Long> {
    // 노출이 설정된 모든 스킬을 카테고리별로 정렬하여 조회 (카테고리 내에서 정렬이 필요하면 Skill Entity에 sortOrder 추가 후 정의)
    List<Skill> findByIsVisibleTrueOrderByCategoryAscNameAsc();

    List<Skill> findAll();
}
