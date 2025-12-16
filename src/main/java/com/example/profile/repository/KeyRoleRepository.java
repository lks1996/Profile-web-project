package com.example.profile.repository;

import com.example.profile.model.KeyRole;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface KeyRoleRepository extends JpaRepository<KeyRole, Long> {
    // 노출이 설정된 핵심 역할만 순서대로 조회
    List<KeyRole> findByIsVisibleTrueOrderBySortOrderAsc();

    List<KeyRole> findAllByOrderBySortOrderAsc();
}
