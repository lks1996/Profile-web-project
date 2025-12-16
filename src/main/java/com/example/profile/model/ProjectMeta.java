package com.example.profile.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "project_meta")
public class ProjectMeta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectMaster projectMaster;
    @Column(name = "item_type", nullable = false)
    private String itemType; // DURATION, TECH_STACK_GROUP, SUMMARY, CONTENT_GROUP
    @Column(columnDefinition = "TEXT")
    private String content;
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
    @Column(name = "is_visible", nullable = false)
    private boolean isVisible = true;

    @OneToMany(mappedBy = "projectMeta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ProjectTechStack> techStacks = new ArrayList<>();;

    @OneToMany(mappedBy = "projectMeta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<Problem> problems = new ArrayList<>();

    public void establishRelationship() {
        // 1. 기술 스택 연결
        if (this.techStacks != null) {
            this.techStacks.removeIf(t -> t.getTechName() == null || t.getTechName().trim().isEmpty());
            this.techStacks.forEach(stack -> stack.setProjectMeta(this));
        }

        // 2. 문제(Episode) 연결
        if (this.problems != null) {
            this.problems.forEach(prob -> {
                prob.setProjectMeta(this);
                prob.establishRelationship(); // 문제야 너도 네 식구 챙겨라
            });
        }
    }
}
