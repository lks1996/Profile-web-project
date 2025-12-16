package com.example.profile.repository;


import com.example.profile.model.ResumeConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeConfigRepository extends JpaRepository<ResumeConfig, Long> {
    // 단일 레코드이므로 기본 findById(1L) 사용
}
