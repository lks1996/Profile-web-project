package com.example.profile.repository;

import com.example.profile.model.ProjectMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProjectMasterRepository extends JpaRepository<ProjectMaster, Long> {
    // 특정 Company에 속하는 노출 프로젝트만 순서대로 조회
    List<ProjectMaster> findByCompanyIdAndIsVisibleTrueOrderBySortOrderAsc(Long companyId);
}
