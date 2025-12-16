package com.example.profile.dto;

import lombok.Data;
import java.util.List;

@Data
public class ProjectGroupDTO {
    private String groupName;
    private int sortOrder;
    private List<ProjectDTO> projects;
}
