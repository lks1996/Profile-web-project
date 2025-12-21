package com.example.profile.service;

import com.example.profile.dto.ProfileWrapper;
import com.example.profile.model.*;
import com.example.profile.repository.*;
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

    private final ProfileConfigRepository configRepo;
    private final ProfileSectionRepository sectionRepo; // 섹션 레포지토리 추가
    private final KeyRoleRepository keyRoleRepo;
    private final SkillCategoryRepository skillCategoryRepo;
    private final CompanyRepository companyRepo;
    private final EducationRepository educationRepo;
    private final CertificationRepository certificationRepo;

    public ProfileWrapper getPublicProfile() {
        ProfileWrapper wrapper = new ProfileWrapper();

        // 0. Sections (섹션 순서 및 노출 여부 결정)
        List<ProfileSection> sections = sectionRepo.findAll().stream()
                .filter(ProfileSection::isVisible) // 숨김 처리된 섹션 제외
                .sorted(Comparator.comparingInt(ProfileSection::getSortOrder)) // 섹션 순서대로 정렬
                .collect(Collectors.toList());
        wrapper.setSections(sections);

        // 1. Config
        wrapper.setConfig(configRepo.findAll().stream().findFirst().orElse(new ProfileConfig()));

        // 2. Key Roles (필터링 + 정렬)
        List<KeyRole> roles = keyRoleRepo.findAll().stream()
                .filter(KeyRole::isVisible)
                .sorted(Comparator.comparingInt(KeyRole::getSortOrder))
                .collect(Collectors.toList());
        wrapper.setKeyRoles(roles);

        // 3. Skills (필터링 + 정렬)
        List<SkillCategory> categories = skillCategoryRepo.findAll().stream()
                .filter(SkillCategory::isVisible)
                .sorted(Comparator.comparingInt(SkillCategory::getSortOrder))
                .peek(cat -> {
                    List<Skill> visibleSkills = cat.getSkills().stream()
                            .filter(Skill::isVisible) // 스킬 개별 숨김 적용
                            .sorted(Comparator.comparingInt(Skill::getSortOrder))
                            .collect(Collectors.toList());
                    cat.setSkills(visibleSkills);
                })
                .collect(Collectors.toList());
        wrapper.setSkillCategories(categories);

        // 4. Experience (필터링 + 정렬)
        List<Company> companies = companyRepo.findAll().stream()
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

        // 5. Education (필터링 + 정렬)
        List<Education> educations = educationRepo.findAll().stream()
                .filter(Education::isVisible)
                .sorted(Comparator.comparingInt(Education::getSortOrder))
                .collect(Collectors.toList());
        wrapper.setEducations(educations);

        // 6. Certification (필터링 + 정렬)
        List<Certification> certifications = certificationRepo.findAll().stream()
                .filter(Certification::isVisible)
                .sorted(Comparator.comparingInt(Certification::getSortOrder))
                .collect(Collectors.toList());
        wrapper.setCertifications(certifications);

        return wrapper;
    }
}