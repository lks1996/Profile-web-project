package com.example.profile.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileMaster {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title; // 이력서 제목 (예: 백엔드 지원용)

    private boolean isActive; // 현재 사용자 페이지에 노출 중인지 여부

    private LocalDateTime lastModifiedDate;

    // 자식 엔티티들과의 관계 설정 (Cascade로 한 번에 저장/삭제)
    @OneToOne(mappedBy = "profileMaster", cascade = CascadeType.ALL, orphanRemoval = true)
    private ProfileConfig config;

    @OneToMany(mappedBy = "profileMaster", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProfileSection> sections = new ArrayList<>();

    @OneToMany(mappedBy = "profileMaster", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<KeyRole> keyRoles = new ArrayList<>();

    @OneToMany(mappedBy = "profileMaster", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SkillCategory> skillCategories = new ArrayList<>();

    @OneToMany(mappedBy = "profileMaster", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Company> companies = new ArrayList<>();

    @OneToMany(mappedBy = "profileMaster", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Education> educations = new ArrayList<>();

    @OneToMany(mappedBy = "profileMaster", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Certification> certifications = new ArrayList<>();
}