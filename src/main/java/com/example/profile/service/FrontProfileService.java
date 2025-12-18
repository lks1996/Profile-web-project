package com.example.profile.service;

import com.example.profile.model.*;
import com.example.profile.dto.*;
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
    private final KeyRoleRepository keyRoleRepo;
    private final SkillCategoryRepository skillCategoryRepo;
    private final CompanyRepository companyRepo;
    private final EducationRepository educationRepo;
    private final CertificationRepository certificationRepo;

    public ProfileWrapper getPublicProfile() {
        ProfileWrapper wrapper = new ProfileWrapper();

        // 1. Config
        wrapper.setConfig(configRepo.findAll().stream().findFirst().orElse(new ProfileConfig()));

        // 2. Key Roles
        List<KeyRole> roles = keyRoleRepo.findAll().stream()
                .filter(KeyRole::isVisible)
                .sorted(Comparator.comparingInt(KeyRole::getSortOrder))
                .collect(Collectors.toList());
        wrapper.setKeyRoles(roles);

        // 3. Skills
        List<SkillCategory> categories = skillCategoryRepo.findAll().stream()
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

        // 4. Company -> Project -> Meta -> (Tech, Problems -> Sol/Imp)
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
                                            // [중요] 여기서 Lazy Loading된 컬렉션들을 강제로 접근해서 초기화해야 합니다.

                                            // 4-1. 기술 스택 (TechStacks) 필터링
                                            if (meta.getTechStacks() != null) {
                                                List<ProjectTechStack> visibleTechs = meta.getTechStacks().stream()
                                                        .filter(ProjectTechStack::isVisible)
                                                        .sorted(Comparator.comparingInt(ProjectTechStack::getSortOrder))
                                                        .collect(Collectors.toList());
                                                meta.setTechStacks(visibleTechs);
                                            }

                                            // 4-2. 문제 해결 (Problems) 초기화 및 하위 필터링
                                            // *Problems 자체는 Visible 필드가 없으므로 모두 가져오되, 그 안의 Solution/Impact는 필터링*
                                            if (meta.getProblems() != null) {
                                                List<Problem> problems = meta.getProblems().stream()
                                                        .sorted(Comparator.comparingInt(Problem::getSortOrder))
                                                        .peek(prob -> {
                                                            // Solutions 필터링
                                                            if (prob.getSolutions() != null) {
                                                                List<Solution> visibleSols = prob.getSolutions().stream()
                                                                        .filter(Solution::isVisible)
                                                                        .sorted(Comparator.comparingInt(Solution::getSortOrder))
                                                                        .collect(Collectors.toList());
                                                                prob.setSolutions(visibleSols);
                                                            }
                                                            // Impacts 필터링
                                                            if (prob.getImpacts() != null) {
                                                                List<Impact> visibleImps = prob.getImpacts().stream()
                                                                        .filter(Impact::isVisible)
                                                                        .sorted(Comparator.comparingInt(Impact::getSortOrder))
                                                                        .collect(Collectors.toList());
                                                                prob.setImpacts(visibleImps);
                                                            }
                                                        })
                                                        .collect(Collectors.toList());
                                                meta.setProblems(problems);
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

        // 5. Education
        List<Education> educations = educationRepo.findAll().stream()
                .filter(Education::isVisible)
                .sorted(Comparator.comparingInt(Education::getSortOrder))
                .collect(Collectors.toList());
        wrapper.setEducations(educations);

        // 6. Certification
        List<Certification> certifications = certificationRepo.findAll().stream()
                .filter(Certification::isVisible)
                .sorted(Comparator.comparingInt(Certification::getSortOrder))
                .collect(Collectors.toList());
        wrapper.setCertifications(certifications);

        return wrapper;
    }
}