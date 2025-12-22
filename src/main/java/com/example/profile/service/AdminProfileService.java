package com.example.profile.service;

import com.example.profile.dto.ProfileWrapper;
import com.example.profile.model.*;
import com.example.profile.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminProfileService {

    private final ProfileMasterRepository masterRepo;

    // =================================================================================
    // 1. 이력서 목록 관리
    // =================================================================================

    @Transactional(readOnly = true)
    public List<ProfileMaster> getAllProfiles() {
        return masterRepo.findAll(Sort.by(Sort.Direction.DESC, "lastModifiedDate"));
    }

    public void createNewProfile(String title) {
        ProfileMaster profile = new ProfileMaster();
        profile.setTitle(title);
        profile.setActive(false);
        profile.setLastModifiedDate(LocalDateTime.now());

        ProfileConfig config = new ProfileConfig();
        config.setProfileMaster(profile);
        profile.setConfig(config);

        List<ProfileSection> sections = new ArrayList<>();
        sections.add(createDefaultSection(profile, SectionType.ABOUT, "About Me", 0));
        sections.add(createDefaultSection(profile, SectionType.SKILLS, "Skills", 1));
        sections.add(createDefaultSection(profile, SectionType.PROJECTS, "Experience", 2));
        sections.add(createDefaultSection(profile, SectionType.EDUCATION, "Education", 3));
        sections.add(createDefaultSection(profile, SectionType.CERTIFICATION, "Certifications", 4));
        profile.setSections(sections);

        masterRepo.save(profile);
    }

    public void setActiveProfile(Long profileId) {
        List<ProfileMaster> all = masterRepo.findAll();
        for (ProfileMaster p : all) {
            p.setActive(p.getId().equals(profileId));
        }
    }

    // =================================================================================
    // 2. 에디터 데이터 조회 및 저장
    // =================================================================================

    @Transactional(readOnly = true)
    public ProfileWrapper getProfileWrapper(Long profileId) {
        ProfileMaster master = masterRepo.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Profile ID: " + profileId));

        ProfileWrapper wrapper = new ProfileWrapper();
        wrapper.setProfileId(master.getId());
        wrapper.setProfileTitle(master.getTitle());
        wrapper.setConfig(master.getConfig());

        // 정렬해서 DTO에 담기
        wrapper.setSections(sortList(master.getSections(), Comparator.comparingInt(ProfileSection::getSortOrder)));
        wrapper.setKeyRoles(sortList(master.getKeyRoles(), Comparator.comparingInt(KeyRole::getSortOrder)));
        wrapper.setSkillCategories(sortList(master.getSkillCategories(), Comparator.comparingInt(SkillCategory::getSortOrder)));
        wrapper.setCompanies(sortList(master.getCompanies(), Comparator.comparingInt(Company::getSortOrder)));
        wrapper.setEducations(sortList(master.getEducations(), Comparator.comparingInt(Education::getSortOrder)));
        wrapper.setCertifications(sortList(master.getCertifications(), Comparator.comparingInt(Certification::getSortOrder)));

        // 기술 스택 감지
        Set<String> projectTechs = initDeepDataAndCollectTechs(master.getCompanies());
        Set<String> registeredSkills = collectRegisteredSkills(master.getSkillCategories());
        projectTechs.removeAll(registeredSkills);
        wrapper.setDetectedSkills(new ArrayList<>(projectTechs));

        return wrapper;
    }

    public void saveProfile(Long profileId, ProfileWrapper wrapper) {
        ProfileMaster master = masterRepo.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Profile ID"));

        // 1. Config 업데이트
        if (wrapper.getConfig() != null) {
            ProfileConfig dbConfig = master.getConfig();
            ProfileConfig formConfig = wrapper.getConfig();
            dbConfig.setFullName(formConfig.getFullName());
            dbConfig.setCompanyRoleLabel(formConfig.getCompanyRoleLabel());
            dbConfig.setAboutParagraph(formConfig.getAboutParagraph());
            dbConfig.setPhone(formConfig.getPhone());
            dbConfig.setEmail(formConfig.getEmail());
            dbConfig.setGithub(formConfig.getGithub());
        }

        // 2. 각 리스트 업데이트 (스마트 병합: ID 유지)
        mergeSections(master.getSections(), wrapper.getSections(), master);
        mergeKeyRoles(master.getKeyRoles(), wrapper.getKeyRoles(), master);
        mergeCompanies(master.getCompanies(), wrapper.getCompanies(), master);
        mergeSkillCategories(master.getSkillCategories(), wrapper.getSkillCategories(), master);
        mergeEducations(master.getEducations(), wrapper.getEducations(), master);
        mergeCertifications(master.getCertifications(), wrapper.getCertifications(), master);

        // 3. 저장
        master.setLastModifiedDate(LocalDateTime.now());
        masterRepo.save(master);
    }

    // =================================================================================
    // 3. 스마트 병합 로직 (ID 보존의 핵심)
    // =================================================================================

    // 공통 로직: 리스트 병합 템플릿
    private <T> void mergeList(List<T> dbList, List<T> formList,
                               Function<T, Long> idGetter,
                               BiConsumer<T, T> updater,
                               BiConsumer<T, ProfileMaster> initializer, // 부모 설정용
                               ProfileMaster master) {
        if (formList == null) formList = new ArrayList<>();

        // 1. 삭제 (DB에는 있는데 Form에는 없는 것)
        Set<Long> formIds = formList.stream()
                .map(idGetter)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        dbList.removeIf(item -> idGetter.apply(item) != null && !formIds.contains(idGetter.apply(item)));

        // 2. 수정 및 추가
        Map<Long, T> dbMap = dbList.stream()
                .collect(Collectors.toMap(idGetter, item -> item));

        for (T formItem : formList) {
            Long id = idGetter.apply(formItem);
            if (id != null && dbMap.containsKey(id)) {
                // [수정] ID가 같으면 내용을 덮어씌움 (객체 교체 X, 필드 값 복사 O)
                updater.accept(dbMap.get(id), formItem);
            } else {
                // [추가] ID가 없거나 DB에 없으면 새로 추가
                initializer.accept(formItem, master); // 부모 설정
                dbList.add(formItem);
            }
        }
    }

    // ----------------------------------------------------------------------
    // 각 엔티티별 구체적인 업데이트 로직 (복사할 필드 지정)
    // ----------------------------------------------------------------------

    private void mergeSections(List<ProfileSection> dbList, List<ProfileSection> formList, ProfileMaster master) {
        mergeList(dbList, formList, ProfileSection::getId,
                (db, form) -> {
                    db.setSectionName(form.getSectionName());
                    db.setSectionType(form.getSectionType()); // 타입도 변경 가능하게 할 경우
                    db.setVisible(form.isVisible());
                    db.setSortOrder(form.getSortOrder());
                },
                (form, m) -> { form.setProfileMaster(m); if(form.getSectionName()==null) form.setSectionName(form.getSectionType().name()); },
                master
        );
    }

    private void mergeKeyRoles(List<KeyRole> dbList, List<KeyRole> formList, ProfileMaster master) {
        if(formList != null) formList.removeIf(r -> isEmpty(r.getRoleContent()));
        mergeList(dbList, formList, KeyRole::getId,
                (db, form) -> {
                    db.setRoleContent(form.getRoleContent());
                    db.setVisible(form.isVisible());
                    db.setSortOrder(form.getSortOrder());
                },
                (form, m) -> form.setProfileMaster(m),
                master
        );
    }

    private void mergeEducations(List<Education> dbList, List<Education> formList, ProfileMaster master) {
        if(formList != null) formList.removeIf(e -> isEmpty(e.getInstitution()));
        mergeList(dbList, formList, Education::getId,
                (db, form) -> {
                    db.setInstitution(form.getInstitution());
                    db.setMajor(form.getMajor());
                    db.setGpa(form.getGpa());
                    db.setPeriod(form.getPeriod());
                    db.setAdditionalInfo(form.getAdditionalInfo());
                    db.setVisible(form.isVisible());
                    db.setSortOrder(form.getSortOrder());
                },
                (form, m) -> form.setProfileMaster(m),
                master
        );
    }

    private void mergeCertifications(List<Certification> dbList, List<Certification> formList, ProfileMaster master) {
        if(formList != null) formList.removeIf(c -> isEmpty(c.getName()));
        mergeList(dbList, formList, Certification::getId,
                (db, form) -> {
                    db.setName(form.getName());
                    db.setIssueDate(form.getIssueDate());
                    db.setAdditionalInfo(form.getAdditionalInfo());
                    db.setVisible(form.isVisible());
                    db.setSortOrder(form.getSortOrder());
                },
                (form, m) -> form.setProfileMaster(m),
                master
        );
    }

    // ★ [중요] 계층 구조가 깊은 Company -> Project -> Meta -> ... 병합
    private void mergeCompanies(List<Company> dbList, List<Company> formList, ProfileMaster master) {
        if(formList != null) formList.removeIf(c -> isEmpty(c.getName()));

        // 1. Company 레벨 병합
        mergeList(dbList, formList, Company::getId,
                (dbComp, formComp) -> {
                    // 필드 업데이트
                    dbComp.setName(formComp.getName());
                    dbComp.setType(formComp.getType());
                    dbComp.setVisible(formComp.isVisible());
                    dbComp.setSortOrder(formComp.getSortOrder());

                    // [재귀 호출] 하위 프로젝트 병합
                    mergeProjects(dbComp.getProjects(), formComp.getProjects(), dbComp);
                },
                (formComp, m) -> {
                    formComp.setProfileMaster(m);
                    formComp.establishRelationship(); // 신규 생성 시 하위 관계 연결
                },
                master
        );
    }

    private void mergeProjects(List<ProjectMaster> dbList, List<ProjectMaster> formList, Company parent) {
        // Project 병합 로직 (Helper 메서드를 못 쓰므로 직접 구현하거나 별도 Helper를 만듦.
        // 여기서는 패턴이 같으므로 Generic Helper를 활용하기 위해 idGetter 등을 맞춤)

        // * Project는 ProfileMaster가 아니라 Company가 부모이므로 mergeList 파라미터가 안 맞음.
        // * 따라서 내부 로직을 풀어 씀.

        if (formList == null) formList = new ArrayList<>();
        Set<Long> formIds = formList.stream().map(ProjectMaster::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        dbList.removeIf(p -> p.getId() != null && !formIds.contains(p.getId()));

        Map<Long, ProjectMaster> dbMap = dbList.stream().collect(Collectors.toMap(ProjectMaster::getId, p -> p));

        for (ProjectMaster formP : formList) {
            if (formP.getId() != null && dbMap.containsKey(formP.getId())) {
                ProjectMaster dbP = dbMap.get(formP.getId());
                dbP.setTitle(formP.getTitle());
                dbP.setSortOrder(formP.getSortOrder());
                dbP.setVisible(formP.isVisible());
                // [재귀] MetaItems 병합
                mergeMetaItems(dbP.getMetaItems(), formP.getMetaItems(), dbP);
            } else {
                formP.setCompany(parent);
                formP.establishRelationship();
                dbList.add(formP);
            }
        }
    }

    private void mergeMetaItems(List<ProjectMeta> dbList, List<ProjectMeta> formList, ProjectMaster parent) {
        if (formList == null) formList = new ArrayList<>();

        // [중요] 메타 아이템 자체 필터링 (타입이 없으면 의미 없음)
        formList.removeIf(m -> m.getItemType() == null);

        Set<Long> formIds = formList.stream().map(ProjectMeta::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        dbList.removeIf(m -> m.getId() != null && !formIds.contains(m.getId()));
        Map<Long, ProjectMeta> dbMap = dbList.stream().collect(Collectors.toMap(ProjectMeta::getId, m -> m));

        for (ProjectMeta formM : formList) {
            if (formM.getId() != null && dbMap.containsKey(formM.getId())) {
                ProjectMeta dbM = dbMap.get(formM.getId());
                dbM.setItemType(formM.getItemType());
                dbM.setContent(formM.getContent());
                dbM.setSortOrder(formM.getSortOrder());
                dbM.setVisible(formM.isVisible());

                // 하위 병합 호출
                mergeTechStacks(dbM.getTechStacks(), formM.getTechStacks(), dbM);
                mergeProblems(dbM.getProblems(), formM.getProblems(), dbM);
            } else {
                // 신규 추가 시에도 하위 리스트 검증 필요하므로 일단 추가 후 내부 정리는 JPA가 하기 전에 리스트가 깨끗해야 함.
                // 하지만 여기선 formM 객체 자체를 넣으므로, formM 내부의 리스트도 정리해줘야 함.
                if (formM.getTechStacks() != null) formM.getTechStacks().removeIf(t -> isEmpty(t.getTechName()));

                // Problem 내부 정리 로직은 복잡하므로, 일단 부모 설정 후 리스트에 추가
                formM.setProjectMaster(parent);
                formM.establishRelationship(); // 여기서 내부 정리된 리스트들의 부모가 설정됨

                // [추가 안전장치] establishRelationship 후 다시 한 번 빈 값 제거
                if (formM.getTechStacks() != null) formM.getTechStacks().removeIf(t -> isEmpty(t.getTechName()));

                dbList.add(formM);
            }
        }
    }

    private void mergeTechStacks(List<ProjectTechStack> dbList, List<ProjectTechStack> formList, ProjectMeta parent) {
        if (formList == null) formList = new ArrayList<>();

        // [핵심 수정] 이름(techName)이 없는 빈 객체(인덱스 채우기용) 제거
        formList.removeIf(t -> isEmpty(t.getTechName()));

        Set<Long> formIds = formList.stream().map(ProjectTechStack::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        dbList.removeIf(t -> t.getId() != null && !formIds.contains(t.getId()));
        Map<Long, ProjectTechStack> dbMap = dbList.stream().collect(Collectors.toMap(ProjectTechStack::getId, t -> t));

        for (ProjectTechStack formT : formList) {
            if (formT.getId() != null && dbMap.containsKey(formT.getId())) {
                ProjectTechStack dbT = dbMap.get(formT.getId());
                dbT.setTechName(formT.getTechName());
                dbT.setSortOrder(formT.getSortOrder());
                dbT.setVisible(formT.isVisible());
            } else {
                formT.setProjectMeta(parent);
                dbList.add(formT);
            }
        }
    }

    private void mergeProblems(List<Problem> dbList, List<Problem> formList, ProjectMeta parent) {
        if (formList == null) formList = new ArrayList<>();

        // 제목 없는 문제 정의 제거
        formList.removeIf(p -> isEmpty(p.getTitle()));

        Set<Long> formIds = formList.stream().map(Problem::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        dbList.removeIf(p -> p.getId() != null && !formIds.contains(p.getId()));
        Map<Long, Problem> dbMap = dbList.stream().collect(Collectors.toMap(Problem::getId, p -> p));

        for (Problem formP : formList) {
            if (formP.getId() != null && dbMap.containsKey(formP.getId())) {
                Problem dbP = dbMap.get(formP.getId());
                dbP.setTitle(formP.getTitle());
                dbP.setSortOrder(formP.getSortOrder());

                mergeSolutions(dbP.getSolutions(), formP.getSolutions(), dbP);
                mergeImpacts(dbP.getImpacts(), formP.getImpacts(), dbP);
            } else {
                formP.setProjectMeta(parent);
                // 신규 추가 시 내부 리스트 필터링
                if (formP.getSolutions() != null) formP.getSolutions().removeIf(s -> isEmpty(s.getContent()));
                if (formP.getImpacts() != null) formP.getImpacts().removeIf(i -> isEmpty(i.getContent()));

                formP.establishRelationship();
                dbList.add(formP);
            }
        }
    }

    private void mergeSolutions(List<Solution> dbList, List<Solution> formList, Problem parent) {
        if (formList == null) formList = new ArrayList<>();

        // [수정] 내용 없는 솔루션 제거
        formList.removeIf(s -> isEmpty(s.getContent()));

        Set<Long> formIds = formList.stream().map(Solution::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        dbList.removeIf(s -> s.getId() != null && !formIds.contains(s.getId()));
        Map<Long, Solution> dbMap = dbList.stream().collect(Collectors.toMap(Solution::getId, s -> s));
        for(Solution formS : formList) {
            if(formS.getId()!=null && dbMap.containsKey(formS.getId())) {
                Solution dbS = dbMap.get(formS.getId());
                dbS.setContent(formS.getContent());
                dbS.setSortOrder(formS.getSortOrder());
                dbS.setVisible(formS.isVisible());
            } else {
                formS.setProblem(parent);
                dbList.add(formS);
            }
        }
    }

    private void mergeImpacts(List<Impact> dbList, List<Impact> formList, Problem parent) {
        if (formList == null) formList = new ArrayList<>();

        // [수정] 내용 없는 성과 제거
        formList.removeIf(i -> isEmpty(i.getContent()));

        Set<Long> formIds = formList.stream().map(Impact::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        dbList.removeIf(i -> i.getId() != null && !formIds.contains(i.getId()));
        Map<Long, Impact> dbMap = dbList.stream().collect(Collectors.toMap(Impact::getId, i -> i));
        for(Impact formI : formList) {
            if(formI.getId()!=null && dbMap.containsKey(formI.getId())) {
                Impact dbI = dbMap.get(formI.getId());
                dbI.setContent(formI.getContent());
                dbI.setSortOrder(formI.getSortOrder());
                dbI.setVisible(formI.isVisible());
            } else {
                formI.setProblem(parent);
                dbList.add(formI);
            }
        }
    }

    private void mergeSkillCategories(List<SkillCategory> dbList, List<SkillCategory> formList, ProfileMaster master) {
        if(formList != null) formList.removeIf(c -> isEmpty(c.getName()));
        mergeList(dbList, formList, SkillCategory::getId,
                (db, form) -> {
                    db.setName(form.getName());
                    db.setSortOrder(form.getSortOrder());
                    db.setVisible(form.isVisible());
                    // 하위 스킬 병합
                    mergeSkills(db.getSkills(), form.getSkills(), db);
                },
                (form, m) -> { form.setProfileMaster(m); form.establishRelationship(); },
                master
        );
    }

    private void mergeSkills(List<Skill> dbList, List<Skill> formList, SkillCategory parent) {
        if (formList == null) formList = new ArrayList<>();
        Set<Long> formIds = formList.stream().map(Skill::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        dbList.removeIf(s -> s.getId() != null && !formIds.contains(s.getId()));
        Map<Long, Skill> dbMap = dbList.stream().collect(Collectors.toMap(Skill::getId, s -> s));

        for (Skill formS : formList) {
            if (formS.getId() != null && dbMap.containsKey(formS.getId())) {
                Skill dbS = dbMap.get(formS.getId());
                dbS.setName(formS.getName());
                dbS.setSortOrder(formS.getSortOrder());
                dbS.setVisible(formS.isVisible());
            } else {
                formS.setCategory(parent);
                dbList.add(formS);
            }
        }
    }

    // =================================================================================
    // 4. 유틸리티
    // =================================================================================

    private ProfileSection createDefaultSection(ProfileMaster profile, SectionType type, String name, int order) {
        ProfileSection section = new ProfileSection();
        section.setProfileMaster(profile);
        section.setSectionType(type);
        section.setSectionName(name);
        section.setSortOrder(order);
        section.setVisible(true);
        return section;
    }

    private <T> List<T> sortList(List<T> list, Comparator<? super T> c) {
        if (list == null) return new ArrayList<>();
        list.sort(c);
        return list;
    }

    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    private Set<String> initDeepDataAndCollectTechs(List<Company> companies) {
        // (기존과 동일하므로 생략하지 않고 그대로 둡니다. 코드 일관성을 위해)
        Set<String> techs = new HashSet<>();
        if (companies == null) return techs;
        for (Company comp : companies) {
            if (comp.getProjects() != null) {
                comp.getProjects().size();
                for (ProjectMaster proj : comp.getProjects()) {
                    if (proj.getMetaItems() != null) {
                        proj.getMetaItems().size();
                        for (ProjectMeta meta : proj.getMetaItems()) {
                            if (meta.getTechStacks() != null) {
                                meta.getTechStacks().size();
                                boolean isChainVisible = comp.isVisible() && proj.isVisible() && meta.isVisible();
                                for (ProjectTechStack stack : meta.getTechStacks()) {
                                    if (isChainVisible && stack.isVisible() && !isEmpty(stack.getTechName())) {
                                        techs.add(stack.getTechName().trim());
                                    }
                                }
                            }
                            if (meta.getProblems() != null) {
                                meta.getProblems().size();
                                meta.getProblems().forEach(p -> {
                                    if(p.getSolutions()!=null) p.getSolutions().size();
                                    if(p.getImpacts()!=null) p.getImpacts().size();
                                });
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

    // 이력서 삭제 (연관된 모든 데이터 Cascade 삭제)
    public void deleteProfile(Long profileId) {
        masterRepo.deleteById(profileId);
    }
}