package com.example.profile.dto;

import lombok.Data;
import java.util.List;

@Data
public class ProblemDTO {
    private String title;
    private int sortOrder;
    private List<String> solutions;
    private List<String> impacts;
}
