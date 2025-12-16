package com.example.profile.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "resume_section")
public class ResumeSection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SectionType sectionType;
    @Column(name = "section_name", nullable = false)
    private String sectionName;
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
    @Column(name = "is_visible", nullable = false)
    private boolean isVisible = true;
}
