package com.example.profile.repository;

import com.example.profile.model.SkillCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SkillCategoryRepository extends JpaRepository<SkillCategory, Long> {
    // 순서(sortOrder) 오름차순으로 정렬해서 가져옴
    List<SkillCategory> findAllByOrderBySortOrderAsc();
}
