package com.example.profile.repository;


import com.example.profile.model.ProfileConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileConfigRepository extends JpaRepository<ProfileConfig, Long> {
    // 단일 레코드이므로 기본 findById(1L) 사용
}
