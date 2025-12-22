package com.example.profile.controller;

import com.example.profile.dto.ProfileWrapper;
import com.example.profile.service.AdminProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor // AdminProfileService 생성자 주입
public class AdminController {

    private final AdminProfileService adminProfileService;

    // 목록 페이지
    @GetMapping("/profiles")
    public String listProfiles(Model model) {
        model.addAttribute("profiles", adminProfileService.getAllProfiles());
        return "admin/profile-list";
    }

    // 신규 생성
    @PostMapping("/profiles/create")
    public String createProfile(@RequestParam String title) {
        adminProfileService.createNewProfile(title);
        return "redirect:/admin/profiles";
    }

    // 활성화 전환
    @PostMapping("/profiles/{id}/active")
    public String toggleActive(@PathVariable Long id) {
        adminProfileService.setActiveProfile(id);
        return "redirect:/admin/profiles";
    }

    // 수정 페이지 이동
    @GetMapping("/profile/{id}")
    public String editProfile(@PathVariable Long id, Model model) {
        ProfileWrapper wrapper = adminProfileService.getProfileWrapper(id);
        model.addAttribute("wrapper", wrapper);
        model.addAttribute("profileId", id); // form action URL용
        return "admin/profile-editor";
    }

    // 저장
    @PostMapping("/profile/{id}/save")
    public String saveProfile(@PathVariable Long id, @ModelAttribute ProfileWrapper wrapper) {
        adminProfileService.saveProfile(id, wrapper);
        return "redirect:/admin/profile/" + id + "?saved=true";
    }

    // 삭제 기능
    @PostMapping("/profiles/{id}/delete")
    public String deleteProfile(@PathVariable Long id) {
        adminProfileService.deleteProfile(id);
        return "redirect:/admin/profiles"; // 삭제 후 목록으로 리다이렉트
    }
}
