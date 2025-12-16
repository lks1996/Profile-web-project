package com.example.profile.dto;

import lombok.Data;

@Data
public class SectionDTO {
    private String sectionName;
    private int sortOrder;
    private Object content; // KeyRoleDTO List, ProjectGroupDTO List 등이 담김
}
