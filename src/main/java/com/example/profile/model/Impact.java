package com.example.profile.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "impact")
public class Impact {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;
    @Column(columnDefinition = "TEXT")
    private String content;
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
    @Column(name = "is_visible", nullable = false)
    private boolean isVisible = true;

}
