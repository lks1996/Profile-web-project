package com.example.profile.repository;

import com.example.profile.model.ProjectTechStack;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProjectTechStackRepository extends JpaRepository<ProjectTechStack, Long> {
    // 특정 ProjectMeta(TECH_STACK_GROUP)에 속하는 노출 스택 항목만 순서대로 조회
    List<ProjectTechStack> findByProjectMetaIdAndIsVisibleTrueOrderBySortOrderAsc(Long projectMetaId);
}
