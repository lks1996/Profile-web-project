package com.example.profile.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "problem")
public class Problem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meta_id", nullable = false)
    private ProjectMeta projectMeta;
    private String title;
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
    @Column(name = "is_visible", nullable = false)
    private boolean isVisible = true;

    @OneToMany(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<Solution> solutions = new ArrayList<>();;

    @OneToMany(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<Impact> impacts = new ArrayList<>();

    public void establishRelationship() {
        // 1. 해결책 연결
        if (this.solutions != null) {
            this.solutions.removeIf(s -> s.getContent() == null || s.getContent().trim().isEmpty());
            this.solutions.forEach(sol -> sol.setProblem(this));
        }

        // 2. 성과 연결
        if (this.impacts != null) {
            this.impacts.removeIf(i -> i.getContent() == null || i.getContent().trim().isEmpty());
            this.impacts.forEach(imp -> imp.setProblem(this));
        }
    }
}
