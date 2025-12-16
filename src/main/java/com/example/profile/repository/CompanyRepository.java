package com.example.profile.repository;

import com.example.profile.model.Company;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    // 노출이 설정된 회사/그룹만 순서대로 조회 (Personal Projects 그룹 포함)
    List<Company> findByIsVisibleTrueOrderBySortOrderAsc();

    @EntityGraph(attributePaths = {"projects"})
    List<Company> findAllByOrderBySortOrderAsc();
}
