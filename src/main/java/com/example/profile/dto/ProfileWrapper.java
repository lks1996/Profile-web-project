package com.example.profile.dto;

import com.example.profile.model.*;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProfileWrapper {
    private Long profileId;      // 현재 수정 중인 이력서의 ID (Form Action URL 생성용)
    private String profileTitle; // 이력서 제목 (화면 표시용)

    private ProfileConfig config;
    private List<ProfileSection> sections = new ArrayList<>();
    private List<KeyRole> keyRoles = new ArrayList<>();
    private List<Company> companies = new ArrayList<>();
    private List<Education> educations = new ArrayList<>();
    private List<Certification> certifications = new ArrayList<>();
    private List<SkillCategory> skillCategories = new ArrayList<>();

    private List<String> detectedSkills = new ArrayList<>();
}
