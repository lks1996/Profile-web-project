package com.example.profile.dto;

import com.example.profile.model.*;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProfileWrapper {
    private ProfileConfig config;
    private List<ProfileSection> sections = new ArrayList<>();
    private List<KeyRole> keyRoles = new ArrayList<>();
    private List<Company> companies = new ArrayList<>();
    private List<Education> educations = new ArrayList<>();
    private List<Certification> certifications = new ArrayList<>();
    private List<SkillCategory> skillCategories = new ArrayList<>();
    private List<String> detectedSkills = new ArrayList<>();
}
