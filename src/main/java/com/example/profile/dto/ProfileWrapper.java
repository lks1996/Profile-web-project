package com.example.profile.dto;

import com.example.profile.model.*;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProfileWrapper {
    private ResumeConfig config;
    private List<ResumeSection> sections = new ArrayList<>();
    private List<KeyRole> keyRoles = new ArrayList<>();
    private List<Company> companies = new ArrayList<>();
    private List<Skill> skills = new ArrayList<>();
    private List<Education> educations = new ArrayList<>();
    private List<Certification> certifications = new ArrayList<>();
}
