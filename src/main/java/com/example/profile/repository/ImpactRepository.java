package com.example.profile.repository;

import com.example.profile.model.Impact;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ImpactRepository extends JpaRepository<Impact, Long> {
    // 특정 Problem에 속하는 노출 Impact 항목만 순서대로 조회
    List<Impact> findByProblemIdAndIsVisibleTrueOrderBySortOrderAsc(Long problemId);
}
