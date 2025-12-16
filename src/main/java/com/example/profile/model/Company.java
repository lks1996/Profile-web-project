package com.example.profile.model;

import jakarta.persistence.Entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "company")
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
    @Column(name = "is_visible", nullable = false)
    private boolean isVisible = true;
    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ProjectMaster> projects;

    public void establishRelationship() {
        if (this.projects != null) {
            this.projects.forEach(project -> {
                project.setCompany(this); // 1. 프로젝트에게 내(Company)가 부모임을 알림
                project.establishRelationship(); // 2. 프로젝트 너도 네 식구들 챙겨라 (위임)
            });
        }
    }
}
