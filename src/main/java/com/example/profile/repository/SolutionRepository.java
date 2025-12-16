package com.example.profile.repository;

import com.example.profile.model.Solution;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SolutionRepository extends JpaRepository<Solution, Long> {
    // 특정 Problem에 속하는 노출 Solution 항목만 순서대로 조회
    List<Solution> findByProblemIdAndIsVisibleTrueOrderBySortOrderAsc(Long problemId);
}
