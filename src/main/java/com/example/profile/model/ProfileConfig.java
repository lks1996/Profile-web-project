package com.example.profile.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "resume_config")
public class ProfileConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String github;
    @Column(name = "about_paragraph", columnDefinition = "TEXT")
    private String aboutParagraph;
    @Column(name = "company_role_label")
    private String companyRoleLabel;
}
