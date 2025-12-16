package com.example.profile.repository;

import com.example.profile.model.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProblemRepository extends JpaRepository<Problem, Long> {
    // 특정 ProjectMeta(CONTENT_GROUP)에 속하는 노출 Problem 항목만 순서대로 조회
    List<Problem> findByProjectMetaIdAndIsVisibleTrueOrderBySortOrderAsc(Long projectMetaId);
}
