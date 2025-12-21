package com.example.profile.controller;

import com.example.profile.model.ProfileConfig;
import com.example.profile.model.ProfileMaster;
import com.example.profile.repository.ProfileMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody; // RequestBody 추가
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
public class ContactApiController {

    private final ProfileMasterRepository masterRepo;

    @PostMapping("/reveal")
    public ResponseEntity<Map<String, String>> revealContactInfo(@RequestBody Map<String, Long> payload) {
        Long profileId = payload.get("id");
        Map<String, String> response = new HashMap<>();

        // ID 유효성 검사
        if (profileId == null) {
            return ResponseEntity.badRequest().build();
        }

        ProfileMaster master = masterRepo.findById(profileId).orElse(null);

        if (master != null && master.getConfig() != null) {
            ProfileConfig config = master.getConfig();
            response.put("phone", config.getPhone() != null ? config.getPhone() : "");
            response.put("email", config.getEmail() != null ? config.getEmail() : "");
        } else {
            response.put("phone", "");
            response.put("email", "");
        }

        return ResponseEntity.ok(response);
    }
}