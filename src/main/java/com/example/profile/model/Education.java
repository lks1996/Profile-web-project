package com.example.profile.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Getter @Setter @NoArgsConstructor
@Table(name = "education")
public class Education {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String institution; // 학교명
    private String period;      // 기간 (예: 2015.03 - 2022.02)
    private String major;       // 전공
    private String gpa;         // 학점

    @Column(columnDefinition = "TEXT")
    private String additionalInfo; // "더 정보를 추가할 수 있는 기능"

    @Column(name = "sort_order")
    private int sortOrder;

    @Column(name = "is_visible")
    private boolean isVisible = true;
}
