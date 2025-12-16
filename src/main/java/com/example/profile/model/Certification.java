package com.example.profile.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "certification")
public class Certification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;        // 자격증명
    private String issueDate;   // 취득일

    @Column(columnDefinition = "TEXT")
    private String additionalInfo; // 추가 정보

    @Column(name = "sort_order")
    private int sortOrder;

    @Column(name = "is_visible")
    private boolean isVisible = true;
}
