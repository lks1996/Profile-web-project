package com.example.profile.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter
public class SkillCategory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; // 예: "Database", "DevOps"

    private Integer sortOrder;

    private boolean isVisible = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id")
    private ProfileMaster profileMaster;

    // 카테고리 안에 포함된 스킬들
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<Skill> skills = new ArrayList<>();

    public void establishRelationship() {
        if (this.skills != null) {
            // 1. 이름 없는 빈 스킬 데이터 제거 (Data Cleansing)
            this.skills.removeIf(skill ->
                    skill.getName() == null || skill.getName().trim().isEmpty());

            // 2. 부모-자식 관계 설정 (Relationship Mapping)
            this.skills.forEach(skill -> {
                skill.setCategory(this); // 자식에게 부모(나)를 주입
            });
        }
    }
}