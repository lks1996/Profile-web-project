package com.example.profile.dto;

import lombok.Data;
import java.util.List;

@Data
public class ProjectDTO {
    private String title;
    private int sortOrder;

    // 기간, 스택 섹션, 소개, 내용 섹션 등이 모두 정렬된 통합 리스트
    private List<ProjectItemDTO> projectItems;
}
