package com.example.profile.repository;

import com.example.profile.model.ResumeSection;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ResumeSectionRepository extends JpaRepository<ResumeSection, Long> {
    // 노출이 설정된 섹션만 순서대로 조회
    List<ResumeSection> findByIsVisibleTrueOrderBySortOrderAsc();

    List<ResumeSection> findAllByOrderBySortOrderAsc();
}
