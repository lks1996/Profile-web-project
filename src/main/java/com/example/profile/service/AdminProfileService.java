package com.example.profile.service;

import com.example.profile.dto.ProfileWrapper;
import com.example.profile.model.*;
import com.example.profile.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor // 생성자 주입 자동화 (Autowired 생략 가능)
@Transactional // 기본적으로 모든 메서드에 트랜잭션 적용
public class AdminProfileService {

    private final ResumeConfigRepository configRepo;
    private final ResumeSectionRepository sectionRepo;
    private final KeyRoleRepository keyRoleRepo;
    private final CompanyRepository companyRepo;
    private final EducationRepository eduRepo;
    private final CertificationRepository certRepo;
    private final SkillCategoryRepository skillCategoryRepo;
    private final SkillRepository skillRepo;

    /**
     * 관리자 페이지용 전체 데이터 조회
     */
    @Transactional
    public ProfileWrapper getProfileData() {
        ProfileWrapper wrapper = new ProfileWrapper();

        // ================================================================
        // 1. ResumeConfig (기본 설정) 초기화
        // ================================================================
        // ID 1번 설정이 없으면 빈 객체를 만들어서 저장 후 가져옴
        ResumeConfig config = configRepo.findById(1L).orElseGet(() -> {
            ResumeConfig newConfig = new ResumeConfig();
            return configRepo.save(newConfig);
        });
        wrapper.setConfig(config);

        // ================================================================
        // 2. Sections (메인 섹션) 초기화
        // 섹션 데이터가 하나도 없으면 기본 5개 섹션 생성.
        // ================================================================
        List<ResumeSection> sections = sectionRepo.findAllByOrderBySortOrderAsc();

        if (sections.isEmpty()) {
            sections = new ArrayList<>();
            sections.add(createDefaultSection(SectionType.ABOUT, "소개", 0));
            sections.add(createDefaultSection(SectionType.PROJECTS, "경력", 1));
            sections.add(createDefaultSection(SectionType.EDUCATION, "학력", 2));
            sections.add(createDefaultSection(SectionType.CERTIFICATION, "자격증", 3));
            sections.add(createDefaultSection(SectionType.SKILLS, "스킬", 4));

            sections = sectionRepo.saveAll(sections);
        }
        wrapper.setSections(sections);

        // ================================================================
        // 3. 나머지 데이터 로딩 (비어있으면 빈 리스트가 들어가니 문제 없음)
        // ================================================================
        wrapper.setKeyRoles(keyRoleRepo.findAllByOrderBySortOrderAsc());
        wrapper.setEducations(eduRepo.findAllByOrderBySortOrderAsc());
        wrapper.setCertifications(certRepo.findAllByOrderBySortOrderAsc());

        List<Company> companies = companyRepo.findAllByOrderBySortOrderAsc();
        wrapper.setCompanies(companies);

        List<SkillCategory> categories = skillCategoryRepo.findAllByOrderBySortOrderAsc();
        wrapper.setSkillCategories(categories);

        // 4. 기술 스택 감지 로직 (이전에 작성한 코드)
        Set<String> projectTechs = initDeepDataAndCollectTechs(companies);
        Set<String> registeredSkills = collectRegisteredSkills(categories);
        projectTechs.removeAll(registeredSkills);
        wrapper.setDetectedSkills(new ArrayList<>(projectTechs));

        return wrapper;
    }

    /**
     * 전체 데이터 저장 (빈 값 필터링 포함)
     */
    public void saveProfile(ProfileWrapper wrapper) {
        // 1. Config 저장
        configRepo.save(wrapper.getConfig());

        // 2. Sections 저장
        if (wrapper.getSections() != null) {
            for (ResumeSection section : wrapper.getSections()) {
                if (section.getSectionName() == null || section.getSectionName().trim().isEmpty()) {
                    section.setSectionName(section.getSectionType().name());
                }
            }
            sectionRepo.saveAll(wrapper.getSections());
        }

        // 3. 주요 경험 (Key Roles) 저장
        if (wrapper.getKeyRoles() != null) {
            // 내용 없는 것 제거
            wrapper.getKeyRoles().removeIf(role ->
                    role.getRoleContent() == null || role.getRoleContent().trim().isEmpty());
            keyRoleRepo.saveAll(wrapper.getKeyRoles());
        }

        // 4. Companies (Projects 포함) 저장
        if (wrapper.getCompanies() != null) {
            wrapper.getCompanies().removeIf(comp ->
                    comp.getName() == null || comp.getName().trim().isEmpty());

            for (Company company : wrapper.getCompanies()) {
                company.establishRelationship(); // 관계 설정
            }
            companyRepo.saveAll(wrapper.getCompanies());
        }

        // 5. 학력 (Education) 저장
        if (wrapper.getEducations() != null) {
            // 학교명이 없는 빈 데이터 제거
            wrapper.getEducations().removeIf(edu ->
                    edu.getInstitution() == null || edu.getInstitution().trim().isEmpty());
            eduRepo.saveAll(wrapper.getEducations());
        }

        // 6. 자격증 (Certification) 저장
        if (wrapper.getCertifications() != null) {
            wrapper.getCertifications().removeIf(cert ->
                    cert.getName() == null || cert.getName().trim().isEmpty());
            certRepo.saveAll(wrapper.getCertifications());
        }

        // 7. Skill Categories 저장
        if (wrapper.getSkillCategories() != null) {
            wrapper.getSkillCategories().removeIf(cat ->
                    cat.getName() == null || cat.getName().trim().isEmpty());

            for (SkillCategory cat : wrapper.getSkillCategories()) {
                cat.establishRelationship(); // 관계 설정
            }
            skillCategoryRepo.saveAll(wrapper.getSkillCategories());
        }
    }

    /**
     * 이력서 양식폼 기본 섹션 생성 메서드
     */
    private ResumeSection createDefaultSection(SectionType type, String name, int order) {
        ResumeSection section = new ResumeSection();
        section.setSectionType(type);
        section.setSectionName(name);
        section.setSortOrder(order);
        section.setVisible(true);
        return section;
    }

    /**
     * 하위 데이터 강제 초기화(Lazy Loading 해소) + 기술 스택 이름 수집
     */
    private Set<String> initDeepDataAndCollectTechs(List<Company> companies) {
        Set<String> techs = new HashSet<>();

        if (companies == null) return techs;

        for (Company comp : companies) {

            // [추가된 부분] 회사가 숨김(false) 상태면, 그 안의 프로젝트 스킬도 수집하지 않음
            if (!comp.isVisible()) continue;

            if (comp.getProjects() != null) {
                for (ProjectMaster proj : comp.getProjects()) {

                    // [기존] 프로젝트가 숨김 상태면 패스
                    if (!proj.isVisible()) continue;

                    // 1) MetaItems 초기화
                    if (proj.getMetaItems() != null) {
                        for (ProjectMeta meta : proj.getMetaItems()) {

                            // [추가된 부분] 메타 아이템(기술스택 그룹) 자체가 숨김이면 패스
                            if (!meta.isVisible()) continue;

                            // 2) TechStack 초기화 및 수집
                            if (meta.getTechStacks() != null) {
                                for (ProjectTechStack stack : meta.getTechStacks()) {

                                    // [기존] 기술 태그 개별 숨김 체크
                                    if (stack.isVisible() && stack.getTechName() != null && !stack.getTechName().trim().isEmpty()) {
                                        techs.add(stack.getTechName().trim());
                                    }
                                }
                            }
                            // ... (Problems 초기화 로직 유지) ...
                            if (meta.getProblems() != null) {
                                meta.getProblems().size();
                                for (Problem prob : meta.getProblems()) {
                                    if (prob.getSolutions() != null) prob.getSolutions().size();
                                    if (prob.getImpacts() != null) prob.getImpacts().size();
                                }
                            }
                        }
                    }
                }
            }
        }
        return techs;
    }

    private Set<String> collectRegisteredSkills(List<SkillCategory> categories) {
        Set<String> registered = new HashSet<>();
        if (categories == null) return registered;
        for (SkillCategory cat : categories) {
            if (cat.getSkills() != null) {
                for (Skill s : cat.getSkills()) {
                    if (s.getName() != null) registered.add(s.getName().trim());
                }
            }
        }
        return registered;
    }
}
