package com.example.profile.repository;

import com.example.profile.model.ProfileSection;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProfileSectionRepository extends JpaRepository<ProfileSection, Long> {
    // 노출이 설정된 섹션만 순서대로 조회
    List<ProfileSection> findByIsVisibleTrueOrderBySortOrderAsc();

    List<ProfileSection> findAllByOrderBySortOrderAsc();
}
