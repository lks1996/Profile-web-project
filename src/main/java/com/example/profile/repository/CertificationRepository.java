package com.example.profile.repository;

import com.example.profile.model.Certification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CertificationRepository extends JpaRepository<Certification, Long> {
    // Controller에서 호출하는 정렬 메서드 정의
    List<Certification> findAllByOrderBySortOrderAsc();
}
