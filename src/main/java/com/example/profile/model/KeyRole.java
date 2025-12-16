package com.example.profile.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "key_role")
public class KeyRole {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "role_content", columnDefinition = "TEXT", nullable = false)
    private String roleContent;
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
    @Column(name = "is_visible", nullable = false)
    private boolean isVisible = true;
}
