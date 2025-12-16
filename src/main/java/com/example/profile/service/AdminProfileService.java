package com.example.profile.service;

import com.example.profile.dto.ProfileWrapper;
import com.example.profile.model.*;
import com.example.profile.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    private final SkillRepository skillRepo;

    /**
     * 관리자 페이지용 전체 데이터 조회
     */
    @Transactional(readOnly = true)
    public ProfileWrapper getProfileData() {
        ProfileWrapper wrapper = new ProfileWrapper();

        // 1. Config
        wrapper.setConfig(configRepo.findById(1L).orElse(new ResumeConfig()));

        // 2. Sections
        wrapper.setSections(sectionRepo.findAllByOrderBySortOrderAsc());
        wrapper.setKeyRoles(keyRoleRepo.findAllByOrderBySortOrderAsc());

        // 3. Companies (Fetch Join으로 projects까지는 가져옴)
        List<Company> companies = companyRepo.findAllByOrderBySortOrderAsc();

        // ================================================================
        // [핵심 수정] 깊은 계층(Deep Nested) 데이터 강제 초기화
        // 트랜잭션이 살아있을 때 하위 리스트들을 .size() 등으로 호출하여 DB에서 로딩시킵니다.
        // ================================================================
        for (Company company : companies) {
            // ProjectMaster는 이미 로딩되었지만, 혹시 모르니 확인
            if (company.getProjects() != null) {
                for (ProjectMaster project : company.getProjects()) {

                    // 1) MetaItems 강제 로딩
                    project.getMetaItems().size();

                    for (ProjectMeta meta : project.getMetaItems()) {
                        // 2) TechStacks 강제 로딩
                        meta.getTechStacks().size();

                        // 3) Problems 강제 로딩
                        meta.getProblems().size();

                        for (Problem problem : meta.getProblems()) {
                            // 4) Solutions & Impacts 강제 로딩
                            problem.getSolutions().size();
                            problem.getImpacts().size();
                        }
                    }
                }
            }
        }

        wrapper.setCompanies(companies);
        // ================================================================

        wrapper.setEducations(eduRepo.findAllByOrderBySortOrderAsc());
        wrapper.setCertifications(certRepo.findAllByOrderBySortOrderAsc());
        wrapper.setSkills(skillRepo.findAll()); // SkillRepository 메서드명 확인 필요

        return wrapper;
    }

    /**
     * 전체 데이터 저장 (빈 값 필터링 포함)
     */
    public void saveProfile(ProfileWrapper wrapper) {
        // 1. Config 저장
        configRepo.save(wrapper.getConfig());

        // 2. Sections 순서 저장
        if (wrapper.getSections() != null) {
            for (ResumeSection section : wrapper.getSections()) {
                // 화면에서 이름을 안 보내줬다면, Enum 이름(예: "ABOUT", "PROJECTS")을 그대로 저장
                if (section.getSectionName() == null || section.getSectionName().trim().isEmpty()) {
                    section.setSectionName(section.getSectionType().name());
                }
            }
            sectionRepo.saveAll(wrapper.getSections());
        }

        // 3. KeyRoles 필터링 및 저장
        if (wrapper.getKeyRoles() != null) {
            wrapper.getKeyRoles().removeIf(role ->
                    role.getRoleContent() == null || role.getRoleContent().trim().isEmpty());
            keyRoleRepo.saveAll(wrapper.getKeyRoles());
        }

        // 4. Companies 저장
        if (wrapper.getCompanies() != null) {
            // 이름 없는 회사 제거
            wrapper.getCompanies().removeIf(comp ->
                    comp.getName() == null || comp.getName().trim().isEmpty());

            // [리팩토링 후] 지저분한 루프가 사라지고, 엔티티에게 책임을 위임합니다.
            for (Company company : wrapper.getCompanies()) {
                company.establishRelationship(); // "관계 맺어라" 명령 한 번이면 끝!
            }

            companyRepo.saveAll(wrapper.getCompanies());
        }

        // 5. Education 필터링 및 저장
        if (wrapper.getEducations() != null) {
            wrapper.getEducations().removeIf(edu ->
                    edu.getInstitution() == null || edu.getInstitution().trim().isEmpty());
            eduRepo.saveAll(wrapper.getEducations());
        }

        // 6. Certification 필터링 및 저장
        if (wrapper.getCertifications() != null) {
            wrapper.getCertifications().removeIf(cert ->
                    cert.getName() == null || cert.getName().trim().isEmpty());
            certRepo.saveAll(wrapper.getCertifications());
        }

        // 7. Skills 필터링 및 저장
        if (wrapper.getSkills() != null) {
            wrapper.getSkills().removeIf(skill ->
                    skill.getName() == null || skill.getName().trim().isEmpty());
            skillRepo.saveAll(wrapper.getSkills());
        }
    }
}
