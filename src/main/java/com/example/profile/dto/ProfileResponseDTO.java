package com.example.profile.dto;

import lombok.Data;
import java.util.List;

@Data
public class ProfileResponseDTO {
    // 1. 헤더 및 정적 정보
    private String fullName;
    private String jobTitle;
    private String email;
    private String phone;
    private String github;

    // 2. 동적 섹션 목록 (About, Projects, Skills 등)
    private List<SectionDTO> sections;
}
