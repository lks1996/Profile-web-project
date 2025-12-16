package com.example.profile.controller;

import com.example.profile.dto.ResumeResponseDTO;
import com.example.profile.service.FrontProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 이력서 데이터를 JSON API로 제공하는 REST Controller.
 */
@RestController
@RequestMapping("/api/v1/resume")
public class ResumeController {

    private final FrontProfileService frontProfileService;

    @Autowired
    public ResumeController(FrontProfileService frontProfileService) {
        this.frontProfileService = frontProfileService;
    }

    /**
     * 전체 이력서 데이터를 DTO 형태로 반환합니다.
     * 모든 정렬, 필터링, 계층 구조는 Service 레이어에서 처리됩니다.
     */
    @GetMapping
    public ResponseEntity<ResumeResponseDTO> getFullResumeData() {
        // Service를 호출하여 필터링 및 정렬된 최종 DTO를 받습니다.
        ResumeResponseDTO resumeData = frontProfileService.getFullResumeData();

        // 데이터가 성공적으로 조회되면 200 OK와 함께 데이터를 반환합니다.
        return ResponseEntity.ok(resumeData);
    }
}
