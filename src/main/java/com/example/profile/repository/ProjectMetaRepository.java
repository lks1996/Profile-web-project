package com.example.profile.repository;

import com.example.profile.model.ProjectMeta;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProjectMetaRepository extends JpaRepository<ProjectMeta, Long> {
    // 특정 ProjectMaster에 속하는 노출 Meta 항목만 순서대로 조회
    List<ProjectMeta> findByProjectMasterIdAndIsVisibleTrueOrderBySortOrderAsc(Long projectMasterId);
}
