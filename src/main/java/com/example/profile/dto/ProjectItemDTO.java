package com.example.profile.dto;

import lombok.Data;
import java.util.List;

@Data
public class ProjectItemDTO {
    private String itemType;
    private int sortOrder;
    private String content;

    // itemType이 TECH_STACK_GROUP일 때 사용
    private List<String> techStacks;

    // itemType이 CONTENT_GROUP일 때 사용
    private List<ProblemDTO> problems;
}
