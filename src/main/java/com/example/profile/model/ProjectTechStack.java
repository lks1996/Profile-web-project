package com.example.profile.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "project_tech_stack")
public class ProjectTechStack {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meta_id", nullable = false)
    private ProjectMeta projectMeta;
    @Column(name = "tech_name", nullable = false)
    private String techName;
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
    @Column(name = "is_visible", nullable = false)
    private boolean isVisible = true;
}
