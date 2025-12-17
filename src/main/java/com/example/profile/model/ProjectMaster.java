package com.example.profile.model;

import jakarta.persistence.Entity;
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
@Table(name = "project_master")
public class ProjectMaster {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
    @Column(name = "title", nullable = false)
    private String title;
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
    @Column(name = "is_visible", nullable = false)
    private boolean isVisible = true;
    @OneToMany(mappedBy = "projectMaster", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ProjectMeta> metaItems = new ArrayList<>();

    public void establishRelationship() {
        // 1. 기본값 안전장치 (Service에 있던 로직 이동)
        if (this.title == null || this.title.trim().isEmpty()) this.title = "이름 없는 프로젝트";

        // 2. 자식들(MetaItems) 챙기기
        if (this.metaItems != null) {
            this.metaItems.forEach(meta -> {
                meta.setProjectMaster(this); // 메타에게 내가 부모임을 알림
                meta.establishRelationship(); // 메타야 너도 네 식구 챙겨라
            });
        }
    }
}
