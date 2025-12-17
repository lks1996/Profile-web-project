package com.example.profile.service;

import com.example.profile.dto.*;
import com.example.profile.model.*;
import com.example.profile.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FrontProfileService {

    // 모든 Repository 주입
    @Autowired private ResumeConfigRepository configRepo;
    @Autowired private ResumeSectionRepository sectionRepo;
    @Autowired private KeyRoleRepository keyRoleRepo;
    @Autowired private CompanyRepository companyRepo;
    @Autowired private ProjectMasterRepository projectMasterRepo;
    @Autowired private SkillCategoryRepository skillCategoryRepo;
    @Autowired private ProblemRepository problemRepo;
    @Autowired private SolutionRepository solutionRepo;
    @Autowired private ImpactRepository impactRepo;
    @Autowired private ProjectMetaRepository projectMetaRepo;
    @Autowired private ProjectTechStackRepository projectTechStackRepo;


    public ResumeResponseDTO getFullResumeData() {
        ResumeResponseDTO response = new ResumeResponseDTO();

        // 1. Config 데이터 매핑 (Entity -> DTO)
        ResumeConfig config = configRepo.findById(1L).orElse(null);
        if (config != null) {
            response.setFullName(config.getFullName());
            response.setJobTitle(config.getCompanyRoleLabel());
            response.setEmail(config.getEmail());
            response.setPhone(config.getPhone());
            response.setGithub(config.getGithub());
        }

        // 2. 동적 섹션 조회 및 매핑
        // isVisible=true인 섹션만 순서대로 조회
        List<ResumeSection> sections = sectionRepo.findByIsVisibleTrueOrderBySortOrderAsc();

        List<SectionDTO> sectionDTOs = sections.stream()
                .map(this::mapSectionToDTO)
                .collect(Collectors.toList());

        response.setSections(sectionDTOs);
        return response;
    }

    // 최상위 섹션 매핑 메서드
    private SectionDTO mapSectionToDTO(ResumeSection section) {
        SectionDTO dto = new SectionDTO();
        dto.setSectionName(section.getSectionName());
        dto.setSortOrder(section.getSortOrder());

        switch (section.getSectionName()) {
            case "ABOUT":
                dto.setContent(buildAboutContent(configRepo.findById(1L).get().getAboutParagraph()));
                break;
            case "PROJECTS":
                dto.setContent(buildProjectContent());
                break;
            case "SKILLS":
                dto.setContent(buildSkillContent());
                break;
            // EDUCATION, CERTIFICATION 등 추가 섹션은 여기에 case 추가 및 로직 구현
            default:
                dto.setContent(null);
        }
        return dto;
    }

    // ---------------------------------------------
    // 각 섹션별 Content 빌드 메서드
    // ---------------------------------------------

    private List<Object> buildAboutContent(String aboutParagraph) {
        // About은 문단과 핵심 역할 리스트로 구성됨
        List<Object> contentList = new ArrayList<>();
        contentList.add(aboutParagraph);

        // isVisible=true인 KeyRole만 순서대로 조회 후 DTO로 변환
        List<KeyRoleDTO> keyRoles = keyRoleRepo.findByIsVisibleTrueOrderBySortOrderAsc().stream()
                .map(e -> { KeyRoleDTO dto = new KeyRoleDTO(); dto.setRoleContent(e.getRoleContent()); return dto; })
                .collect(Collectors.toList());
        contentList.add(keyRoles);
        return contentList;
    }

    private List<ProjectGroupDTO> buildProjectContent() {
        // isVisible=true인 Company 그룹만 순서대로 조회
        return companyRepo.findByIsVisibleTrueOrderBySortOrderAsc().stream()
                .map(this::mapCompanyToProjectGroupDTO)
                .collect(Collectors.toList());
    }

    private List<Object> buildSkillContent() {
        // 1. 정렬된 카테고리 목록 조회
        // (findAllByOrderBySortOrderAsc 메서드는 AdminProfileService 때문에 이미 Repository에 있을 것입니다)
        List<SkillCategory> categories = skillCategoryRepo.findAllByOrderBySortOrderAsc();

        // 2. 스트림을 사용하여 데이터 변환
        return categories.stream()
                // 2-1. 카테고리 자체가 '노출(Visible)' 상태인 것만 통과
                .filter(SkillCategory::isVisible)
                .map(cat -> {
                    // 2-2. 해당 카테고리 안에서 '노출(Visible)' 상태인 스킬 이름만 추출
                    List<String> validSkills = cat.getSkills().stream()
                            .filter(Skill::isVisible)
                            .map(Skill::getName)
                            .collect(Collectors.toList());

                    // 2-3. 노출할 스킬이 하나도 없으면 이 카테고리는 무시(null 반환)
                    if (validSkills.isEmpty()) {
                        return null;
                    }

                    // 2-4. 기존 포맷 유지: Object[] { "카테고리명", "[스킬1, 스킬2, ...]" }
                    // 프론트엔드(Mustache/Thymeleaf)가 이 배열 구조를 쓰고 있다면 에러 없이 동작할 것입니다.
                    return new String[] { cat.getName(), validSkills.toString() };
                })
                // 2-5. 위에서 null로 반환된(스킬 없는) 카테고리 제거
                .filter(obj -> obj != null)
                .collect(Collectors.toList());
    }

    // ---------------------------------------------
    // 프로젝트 계층 구조 매핑 메서드
    // ---------------------------------------------

    // Company Entity -> ProjectGroupDTO 매핑
    private ProjectGroupDTO mapCompanyToProjectGroupDTO(Company company) {
        ProjectGroupDTO dto = new ProjectGroupDTO();
        dto.setGroupName(company.getName());
        dto.setSortOrder(company.getSortOrder());

        // ProjectMaster Entity 리스트를 필터링하고 ProjectDTO로 변환
        List<ProjectDTO> projects = company.getProjects().stream()
                .filter(ProjectMaster::isVisible)
                .map(this::mapProjectToDTO)
                .collect(Collectors.toList());

        dto.setProjects(projects);
        return dto;
    }

    // ProjectMaster Entity -> ProjectDTO 매핑
    private ProjectDTO mapProjectToDTO(ProjectMaster master) {
        ProjectDTO dto = new ProjectDTO();
        dto.setTitle(master.getTitle());
        dto.setSortOrder(master.getSortOrder());

        // ProjectMeta 리스트를 ProjectItemDTO로 변환 (기간, 스택 섹션, 내용 섹션 등)
        List<ProjectItemDTO> items = master.getMetaItems().stream()
                .filter(ProjectMeta::isVisible)
                .map(this::mapProjectMetaToProjectItemDTO)
                .collect(Collectors.toList());

        dto.setProjectItems(items);
        return dto;
    }

    // ProjectMeta Entity -> ProjectItemDTO 매핑 (가장 복잡한 계층 처리)
    private ProjectItemDTO mapProjectMetaToProjectItemDTO(ProjectMeta meta) {
        ProjectItemDTO dto = new ProjectItemDTO();
        dto.setItemType(meta.getItemType());
        dto.setSortOrder(meta.getSortOrder());
        dto.setContent(meta.getContent());

        if ("TECH_STACK_GROUP".equals(meta.getItemType())) {
            // ProjectTechStack Entity 리스트를 단순 String 리스트로 변환
            List<String> stacks = meta.getTechStacks().stream()
                    .filter(ProjectTechStack::isVisible)
                    .map(ProjectTechStack::getTechName)
                    .collect(Collectors.toList());
            dto.setTechStacks(stacks);

        } else if ("CONTENT_GROUP".equals(meta.getItemType())) {
            // Problem Entity 리스트를 ProblemDTO 리스트로 변환
            List<ProblemDTO> problems = meta.getProblems().stream()
                    .filter(Problem::isVisible)
                    .map(this::mapProblemToDTO)
                    .collect(Collectors.toList());
            dto.setProblems(problems);
        }
        return dto;
    }

    // Problem Entity -> ProblemDTO 매핑
    private ProblemDTO mapProblemToDTO(Problem problem) {
        ProblemDTO dto = new ProblemDTO();
        dto.setTitle(problem.getTitle());
        dto.setSortOrder(problem.getSortOrder());

        // Solution Entity 리스트를 단순 String 리스트로 변환
        dto.setSolutions(problem.getSolutions().stream()
                .filter(Solution::isVisible)
                .map(Solution::getContent)
                .collect(Collectors.toList()));

        // Impact Entity 리스트를 단순 String 리스트로 변환
        dto.setImpacts(problem.getImpacts().stream()
                .filter(Impact::isVisible)
                .map(Impact::getContent)
                .collect(Collectors.toList()));

        return dto;
    }
}
