package com.example.profile.service;

import com.example.profile.dto.ProfileWrapper;
import com.example.profile.model.*;
import com.example.profile.repository.ProfileMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FrontProfileService {

    private final ProfileMasterRepository masterRepo;

    public ProfileWrapper getPublicProfile() {
        ProfileWrapper wrapper = new ProfileWrapper();

        // 1. 활성화된(Active) 이력서 찾기
        // (만약 활성 이력서가 없으면 빈 껍데기 반환)
        ProfileMaster activeProfile = masterRepo.findAll().stream()
                .filter(ProfileMaster::isActive)
                .findFirst()
                .orElse(null);

        if (activeProfile == null) {
            return wrapper; // 빈 객체 반환 (화면에 아무것도 안 나옴)
        }

        wrapper.setProfileId(activeProfile.getId());
        wrapper.setProfileTitle(activeProfile.getTitle());

        // =====================================================================
        // 여기서부터는 'activeProfile' 안에 있는 리스트만 꺼내서 필터링/정렬합니다.
        // =====================================================================

        // 2. Config (1:1 관계이므로 바로 가져옴)
        wrapper.setConfig(activeProfile.getConfig());

        // 3. Sections
        List<ProfileSection> sections = activeProfile.getSections().stream()
                .filter(ProfileSection::isVisible)
                .sorted(Comparator.comparingInt(ProfileSection::getSortOrder))
                .collect(Collectors.toList());
        wrapper.setSections(sections);

        // 4. Key Roles
        List<KeyRole> roles = activeProfile.getKeyRoles().stream()
                .filter(KeyRole::isVisible)
                .sorted(Comparator.comparingInt(KeyRole::getSortOrder))
                .collect(Collectors.toList());
        wrapper.setKeyRoles(roles);

        // 5. Skills
        List<SkillCategory> categories = activeProfile.getSkillCategories().stream()
                .filter(SkillCategory::isVisible)
                .sorted(Comparator.comparingInt(SkillCategory::getSortOrder))
                .peek(cat -> {
                    List<Skill> visibleSkills = cat.getSkills().stream()
                            .filter(Skill::isVisible)
                            .sorted(Comparator.comparingInt(Skill::getSortOrder))
                            .collect(Collectors.toList());
                    cat.setSkills(visibleSkills);
                })
                .collect(Collectors.toList());
        wrapper.setSkillCategories(categories);

        // 6. Experience (Deep Filtering)
        List<Company> companies = activeProfile.getCompanies().stream()
                .filter(Company::isVisible)
                .sorted(Comparator.comparingInt(Company::getSortOrder))
                .peek(comp -> {
                    List<ProjectMaster> visibleProjects = comp.getProjects().stream()
                            .filter(ProjectMaster::isVisible)
                            .sorted(Comparator.comparingInt(ProjectMaster::getSortOrder))
                            .peek(proj -> {
                                List<ProjectMeta> visibleMeta = proj.getMetaItems().stream()
                                        .filter(ProjectMeta::isVisible)
                                        .sorted(Comparator.comparingInt(ProjectMeta::getSortOrder))
                                        .peek(meta -> {
                                            if (meta.getTechStacks() != null) {
                                                meta.setTechStacks(meta.getTechStacks().stream()
                                                        .filter(ProjectTechStack::isVisible)
                                                        .sorted(Comparator.comparingInt(ProjectTechStack::getSortOrder))
                                                        .collect(Collectors.toList()));
                                            }
                                            if (meta.getProblems() != null) {
                                                meta.setProblems(meta.getProblems().stream()
                                                        .sorted(Comparator.comparingInt(Problem::getSortOrder))
                                                        .peek(prob -> {
                                                            if(prob.getSolutions() != null) prob.setSolutions(prob.getSolutions().stream().filter(Solution::isVisible).sorted(Comparator.comparingInt(Solution::getSortOrder)).collect(Collectors.toList()));
                                                            if(prob.getImpacts() != null) prob.setImpacts(prob.getImpacts().stream().filter(Impact::isVisible).sorted(Comparator.comparingInt(Impact::getSortOrder)).collect(Collectors.toList()));
                                                        })
                                                        .collect(Collectors.toList()));
                                            }
                                        })
                                        .collect(Collectors.toList());
                                proj.setMetaItems(visibleMeta);
                            })
                            .collect(Collectors.toList());
                    comp.setProjects(visibleProjects);
                })
                .collect(Collectors.toList());
        wrapper.setCompanies(companies);

        // 7. Education
        List<Education> educations = activeProfile.getEducations().stream()
                .filter(Education::isVisible)
                .sorted(Comparator.comparingInt(Education::getSortOrder))
                .collect(Collectors.toList());
        wrapper.setEducations(educations);

        // 8. Certification
        List<Certification> certifications = activeProfile.getCertifications().stream()
                .filter(Certification::isVisible)
                .sorted(Comparator.comparingInt(Certification::getSortOrder))
                .collect(Collectors.toList());
        wrapper.setCertifications(certifications);

        return wrapper;
    }
}