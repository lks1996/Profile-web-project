package com.example.profile.controller;

import com.example.profile.dto.ProfileWrapper;
import com.example.profile.service.AdminProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/profile")
@RequiredArgsConstructor // AdminProfileService 생성자 주입
public class AdminController {

    private final AdminProfileService adminProfileService;

    @GetMapping("/editor")
    public String showProfileEditor(Model model) {
        // 서비스에서 데이터 가져오기 (트랜잭션 처리됨)
        ProfileWrapper wrapper = adminProfileService.getProfileData();

        model.addAttribute("wrapper", wrapper);
        return "admin/profile-editor";
    }

    @PostMapping("/save")
    public String saveProfile(@ModelAttribute ProfileWrapper wrapper) {
        // 서비스에 저장 요청 (필터링 및 트랜잭션 처리됨)
        adminProfileService.saveProfile(wrapper);

        return "redirect:/admin/profile/editor?saved=true";
    }
}
