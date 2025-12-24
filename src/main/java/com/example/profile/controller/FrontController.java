package com.example.profile.controller;

import com.example.profile.dto.ProfileWrapper;
import com.example.profile.service.FrontProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Controller
@RequiredArgsConstructor
public class FrontController {

    private final FrontProfileService frontProfileService;

    @GetMapping("/")
    public String index(Model model) {
        // 1. 화면에 보여줄 데이터 조회 (숨김 처리된 것 제외됨)
        ProfileWrapper profile = frontProfileService.getPublicProfile();
        log.info("Somebody got profile");

        // 2. 모델에 담기
        model.addAttribute("profile", profile);

        // 3. 뷰 반환 (resources/templates/index.html)
        return "index";
    }
}