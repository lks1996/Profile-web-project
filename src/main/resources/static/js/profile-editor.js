// 수동으로 추가된 스킬들을 기억할 저장소
const manualAddedSkills = new Set();

document.addEventListener("DOMContentLoaded", function() {
    initAllSortables();

    // 페이지 로드 시 '기존 저장된 스킬'을 수동 목록으로 복구
    document.querySelectorAll('#skillCategoryList .skill-item').forEach(item => {
        const name = getSkillNameFromElement(item);
        if (name) {
            manualAddedSkills.add(name);
        }
    });

    // 그 다음 동기화 실행
    syncAllSkills();

    // 이벤트 리스너: 체크박스 변경 감지
    document.addEventListener('change', function(e) {
        if (e.target.matches('input[type="checkbox"]')) {
            if (e.target.nextElementSibling && e.target.nextElementSibling.type === 'hidden') {
                e.target.nextElementSibling.value = e.target.checked;
            }
            syncAllSkills();
        }
    });

    // 이벤트 리스너: 타이핑 감지
    document.body.addEventListener('input', debounce(syncAllSkills, 300));

    // 이벤트 리스너: 클릭 감지 (삭제 등)
    document.body.addEventListener('click', function(e) {
        if (e.target.closest('.btn') || e.target.closest('.bi-x-circle-fill')) {
            setTimeout(syncAllSkills, 100);
        }
    });
});

// 디바운스 함수
function debounce(func, wait) {
    let timeout;
    return function(...args) {
        clearTimeout(timeout);
        timeout = setTimeout(() => func.apply(this, args), wait);
    };
}

// [스킬 동기화 함수]
function syncAllSkills() {
    if (document.querySelector('.sortable-ghost')) return;

    const validSourceSkills = new Set();

    // 1-1. 프로젝트 섹션 스캔
    document.querySelectorAll('#companyList > .list-item').forEach(comp => {
        if (!checkVisible(comp)) return;
        comp.querySelectorAll('.project-item').forEach(proj => {
            if (!checkVisible(proj)) return;
            proj.querySelectorAll('.meta-item').forEach(meta => {
                const typeInput = meta.querySelector('input[name*="itemType"]');
                if (!typeInput || typeInput.value !== 'TECH_STACK_GROUP') return;
                if (!checkVisible(meta)) return;

                meta.querySelectorAll('.tech-item').forEach(tech => {
                    const toggle = tech.querySelector('input[type="checkbox"]');
                    if (toggle && !toggle.checked) return;

                    const nameInput = tech.querySelector('input[type="text"]');
                    if (nameInput && nameInput.value.trim() !== '') {
                        validSourceSkills.add(nameInput.value.trim());
                    }
                });
            });
        });
    });

    // 1-2. 수동 입력 스킬 합치기
    manualAddedSkills.forEach(skill => validSourceSkills.add(skill));

    // 2. [Category 청소]
    document.querySelectorAll('#skillCategoryList .skill-item').forEach(item => {
        const name = getSkillNameFromElement(item);
        if (!validSourceSkills.has(name)) {
            item.remove();
        }
    });

    // 3. [Pool 계산]
    const currentlyAssigned = new Set();
    document.querySelectorAll('#skillCategoryList .skill-item').forEach(item => {
        currentlyAssigned.add(getSkillNameFromElement(item));
    });

    const poolSkills = [...validSourceSkills].filter(x => !currentlyAssigned.has(x));

    // 4. [Render]
    const poolContainer = document.getElementById('detectedSkillPool');
    if(!poolContainer) return;

    poolContainer.innerHTML = '';
    poolSkills.forEach(tech => {
        const html = `
        <div class="skill-chip badge bg-white text-dark border p-2 shadow-sm" style="cursor: move;">
            <span class="skill-name">${tech}</span>
            <input type="hidden" class="skill-name-input" value="${tech}">
        </div>`;
        poolContainer.insertAdjacentHTML('beforeend', html);
    });
}

function getSkillNameFromElement(item) {
    const realNameInput = item.querySelector('.real-name-input');
    const hiddenNameInput = item.querySelector('input[name*=".name"]');
    const simpleInput = item.querySelector('.skill-name-input');

    if (realNameInput) return realNameInput.value;
    if (hiddenNameInput) return hiddenNameInput.value;
    if (simpleInput) return simpleInput.value;
    return item.innerText.trim();
}

function checkVisible(element) {
    const header = element.querySelector('.d-flex');
    if(!header) return true;
    const checkbox = header.querySelector('.form-check-input[type="checkbox"]');
    return checkbox ? checkbox.checked : true;
}

// ========================================================================
// Sortable 초기화 및 유틸
// ========================================================================
function initAllSortables() {
    initSortableContainer('mainSectionList', '.drag-handle-root', '.root-sort-order');
    document.querySelectorAll('.sortable-list').forEach(el => initSortableElement(el, '.drag-handle', '.sort-order'));

    const subSelectors = [
        { cls: '.project-sortable-list', input: '.project-sort-order' },
        { cls: '.meta-sortable-list', input: '.meta-sort-order' },
        { cls: '.tech-sortable-list', input: '.tech-sort-order' },
        { cls: '.solution-sortable-list', input: '.solution-sort-order' },
        { cls: '.impact-sortable-list', input: '.impact-sort-order' }
    ];

    subSelectors.forEach(sel => {
        document.querySelectorAll(sel.cls).forEach(el => initSortableElement(el, '.drag-handle', sel.input));
    });

    // [카테고리 리스트 정렬 초기화]
    const skillCatList = document.getElementById('skillCategoryList');
    if (skillCatList && !skillCatList.getAttribute('data-sortable-init')) {
        new Sortable(skillCatList, {
            handle: '.drag-handle',
            animation: 150,
            ghostClass: 'sortable-ghost',
            onEnd: function () {
                updateSortOrders(skillCatList, '.sort-order');
                updateSkillIndices(); // ★ 카테고리 순서 변경 시 내부 스킬 인덱스도 갱신
            }
        });
        skillCatList.setAttribute('data-sortable-init', 'true');
    }

    initSkillSortables();
}

function initSortableContainer(id, handle, input) {
    const el = document.getElementById(id);
    if(el) initSortableElement(el, handle, input);
}

function initSortableElement(el, handle, inputSelector) {
    if(el.getAttribute('data-sortable-init')) return;
    new Sortable(el, {
        handle: handle,
        animation: 150,
        ghostClass: 'sortable-ghost',
        onEnd: function () { updateSortOrders(el, inputSelector); }
    });
    el.setAttribute('data-sortable-init', 'true');
}

function initSkillSortables() {
    const poolEl = document.getElementById('detectedSkillPool');
    if (poolEl && !poolEl.getAttribute('data-init')) {
        new Sortable(poolEl, {
            group: { name: 'shared-skills', pull: true, put: true },
            animation: 150,
            sort: false,
            ghostClass: 'sortable-ghost',
            onEnd: function() { setTimeout(syncAllSkills, 50); }
        });
        poolEl.setAttribute('data-init', 'true');
    }

    document.querySelectorAll('.skill-sortable-list').forEach(el => {
        new Sortable(el, {
            group: 'shared-skills',
            animation: 150,
            ghostClass: 'bg-info',
            onEnd: function (evt) {
                if (evt.from !== evt.to) {
                    const itemEl = evt.item;
                    // 이동 시 ID/Version 초기화 (충돌 방지)
                    const idInput = itemEl.querySelector('input[name*=".id"]');
                    if (idInput) idInput.value = "";
                    const verInput = itemEl.querySelector('input[name*=".version"]');
                    if (verInput) verInput.value = "";
                }
                updateSkillIndices();
            },
            onAdd: function (evt) {
                transformChipToInput(evt.item);
                updateSkillIndices();
            }
        });
    });
}

function transformChipToInput(item) {
    if (item.classList.contains('skill-item-active')) return;
    const skillName = getSkillNameFromElement(item);

    item.className = "skill-item badge bg-primary text-white p-2 d-flex align-items-center skill-item-active";
    item.style.cursor = "move";
    item.innerHTML = `
        <input type="hidden" class="skill-sort-order" value="99">
        <input type="hidden" name="visible" value="true">
        <span class="me-2">${skillName}</span>
        <input type="hidden" class="real-name-input" value="${skillName}">
        <input type="hidden" class="skill-id" value="">
        <input type="hidden" class="skill-version" value="">
        <i class="bi bi-x-circle-fill ms-1 text-white-50" style="cursor: pointer;" onclick="removeElement(this)"></i>
    `;
}

function addManualSkillToPool() {
    const input = document.getElementById('manualSkillInput');
    const val = input.value.trim();
    if(!val) return;
    manualAddedSkills.add(val);
    syncAllSkills();
    input.value = '';
}

// [핵심 수정 함수] 기존 스킬 + 신규 스킬 모두 인덱스 업데이트
function updateSkillIndices() {
    const categories = document.getElementById('skillCategoryList').children;

    Array.from(categories).forEach((cat, cIdx) => {
        // 1. 카테고리 자체 인덱스
        const catNameInput = cat.querySelector('.card-header input[name*=".name"]');
        if(catNameInput) catNameInput.name = `skillCategories[${cIdx}].name`;

        const catVisInput = cat.querySelector('.card-header input[name*=".visible"]');
        if(catVisInput) catVisInput.name = `skillCategories[${cIdx}].visible`;

        const catSortInput = cat.querySelector('.card-header .sort-order');
        if(catSortInput) {
            catSortInput.value = cIdx;
            catSortInput.name = `skillCategories[${cIdx}].sortOrder`;
        }

        const catIdInput = cat.querySelector('.card-header input[name*=".id"]');
        if(catIdInput) catIdInput.name = `skillCategories[${cIdx}].id`;

        // 2. 내부 스킬 인덱스
        const skillList = cat.querySelector('.skill-sortable-list');
        if (skillList) {
            Array.from(skillList.children).forEach((skill, sIdx) => {

                // (A) SortOrder
                const sortInput = skill.querySelector('.skill-sort-order');
                if(sortInput) {
                    sortInput.value = sIdx;
                    sortInput.name = `skillCategories[${cIdx}].skills[${sIdx}].sortOrder`;
                }

                // (B) Name - ★[수정] 기존(Thymeleaf) input과 신규(JS) input 모두 찾아서 처리
                // .real-name-input은 신규 항목, input[name*=".name"]은 기존 항목
                const nameInput = skill.querySelector('.real-name-input') || skill.querySelector('input[name*=".name"]');
                if(nameInput) nameInput.name = `skillCategories[${cIdx}].skills[${sIdx}].name`;

                // (C) ID - ★[수정] 기존/신규 모두 처리
                const idInput = skill.querySelector('.skill-id') || skill.querySelector('input[name*=".id"]');
                if(idInput) idInput.name = `skillCategories[${cIdx}].skills[${sIdx}].id`;

                // (D) Version - ★[수정] 기존/신규 모두 처리
                const versionInput = skill.querySelector('.skill-version') || skill.querySelector('input[name*=".version"]');
                if(versionInput) versionInput.name = `skillCategories[${cIdx}].skills[${sIdx}].version`;

                // (E) Visible
                // name="visible" (신규) 또는 name="...visible" (기존)
                const visInput = skill.querySelector('input[name="visible"]') || skill.querySelector('input[name*=".visible"]');
                if(visInput) visInput.name = `skillCategories[${cIdx}].skills[${sIdx}].visible`;
            });
        }
    });
}

// 폼 제출 시 인덱스 정리
document.getElementById('profileForm').addEventListener('submit', function() {
    updateSkillIndices();
    const skillCatList = document.getElementById('skillCategoryList');
    if(skillCatList) updateSortOrders(skillCatList, '.sort-order');

    updateSortOrders(document.getElementById('mainSectionList'), '.root-sort-order');

    ['keyRoleList', 'companyList', 'eduList', 'certList'].forEach(id => {
        const el = document.getElementById(id);
        if(el) updateSortOrders(el, '.sort-order');
    });

    document.querySelectorAll('.project-sortable-list').forEach(el => updateSortOrders(el, '.project-sort-order'));
    document.querySelectorAll('.meta-sortable-list').forEach(el => updateSortOrders(el, '.meta-sort-order'));
    document.querySelectorAll('.tech-sortable-list').forEach(el => updateSortOrders(el, '.tech-sort-order'));
    document.querySelectorAll('.solution-sortable-list').forEach(el => updateSortOrders(el, '.solution-sort-order'));
    document.querySelectorAll('.impact-sortable-list').forEach(el => updateSortOrders(el, '.impact-sort-order'));
});

function updateSortOrders(container, inputSelector) {
    if(!container) return;
    Array.from(container.children).forEach((item, index) => {
        const input = item.querySelector(inputSelector);
        if(input) input.value = index;
    });
}

function getSafeIndex(containerId, namePrefix) {
    const container = document.getElementById(containerId);
    if (!container) return 0;
    const inputs = container.getElementsByTagName('input');
    let max = -1;
    const searchStr = namePrefix + "[";
    for(let i=0; i<inputs.length; i++) {
        const name = inputs[i].name;
        if(name && name.startsWith(searchStr)) {
            const rest = name.substring(searchStr.length);
            const closeBracket = rest.indexOf("]");
            if(closeBracket > -1) {
                const idx = parseInt(rest.substring(0, closeBracket));
                if(!isNaN(idx) && idx > max) max = idx;
            }
        }
    }
    return max + 1;
}

function removeElement(btn) {
    if(confirm('정말 삭제하시겠습니까?')) {
        let target =
            btn.closest('.key-role-item') ||
            btn.closest('.skill-item') ||
            btn.closest('.impact-item') ||
            btn.closest('.solution-item') ||
            btn.closest('.tech-item') ||
            btn.closest('.episode-item') ||
            btn.closest('.meta-item') ||
            btn.closest('.project-item') ||
            btn.closest('.list-item');

        if(target) {
            target.remove();
            setTimeout(() => {
                if (typeof updateSkillIndices === 'function') updateSkillIndices();
                if (typeof syncAllSkills === 'function') syncAllSkills();

                const keyRoleList = document.getElementById('keyRoleList');
                if(keyRoleList) updateSortOrders(keyRoleList, '.sort-order');

                document.querySelectorAll('.project-sortable-list').forEach(el => updateSortOrders(el, '.project-sort-order'));
                document.querySelectorAll('.meta-sortable-list').forEach(el => updateSortOrders(el, '.meta-sort-order'));
            }, 50);
        }
    }
}

// --------------------------------------------------------------------------------
// [HTML 생성 함수들] - 동적 추가
// --------------------------------------------------------------------------------
// (이하 함수들은 변경사항 없습니다. 기존 소스와 동일하게 유지하시면 됩니다.)
function addKeyRole() {
    const idx = getSafeIndex('keyRoleList', 'keyRoles');
    const html = `
        <div class="list-item key-role-item d-flex align-items-center mb-2 bg-white p-2 border rounded">
            <i class="bi bi-grip-vertical drag-handle me-2 text-secondary"></i>
            <input type="hidden" name="keyRoles[${idx}].sortOrder" class="sort-order" value="99">
            <div class="flex-grow-1 me-2">
                <textarea name="keyRoles[${idx}].roleContent" class="form-control border-0 bg-transparent" rows="2" style="resize:none;" placeholder="핵심 경험을 요약해서 작성하세요"></textarea>
            </div>
            <div class="form-check form-switch me-2">
                <input class="form-check-input" type="checkbox" checked onchange="this.nextElementSibling.value = this.checked">
                <input type="hidden" name="keyRoles[${idx}].visible" value="true">
            </div>
            <button type="button" class="btn btn-outline-danger btn-sm border-0" onclick="removeElement(this)">
                <i class="bi bi-x-lg"></i>
            </button>
        </div>`;
    document.getElementById('keyRoleList').insertAdjacentHTML('beforeend', html);
}

function addSkillCategory() {
    const idx = getSafeIndex('skillCategoryList', 'skillCategories');
    const html = `
        <div class="list-item card mb-3 bg-white border">
            <div class="card-header bg-light d-flex align-items-center py-2">
                <i class="bi bi-grip-vertical drag-handle me-2 text-secondary"></i>
                <input type="hidden" name="skillCategories[${idx}].sortOrder" class="sort-order" value="99">

                <input type="text" name="skillCategories[${idx}].name" 
                       class="form-control form-control-sm fw-bold border-0 bg-transparent" 
                       style="width: 300px;" 
                       placeholder="새 카테고리">

                <div class="ms-auto d-flex align-items-center">
                    <div class="form-check form-switch me-2">
                        <input class="form-check-input" type="checkbox" checked onchange="this.nextElementSibling.value=this.checked">
                        <input type="hidden" name="skillCategories[${idx}].visible" value="true">
                    </div>
                    <button type="button" class="btn btn-xs btn-outline-danger border-0" onclick="removeElement(this)"><i class="bi bi-trash"></i></button>
                </div>
            </div>
            <div class="card-body p-2">
                <div id="catSkillList-${idx}" class="skill-sortable-list d-flex flex-wrap gap-2" style="min-height: 40px;"></div>
                <div class="text-center mt-2"><small class="text-muted" style="font-size: 0.7rem;">여기로 스킬을 드래그하세요</small></div>
            </div>
        </div>`;
    document.getElementById('skillCategoryList').insertAdjacentHTML('beforeend', html);
    initSkillSortables();
}

function addCompany() {
    const idx = getSafeIndex('companyList', 'companies');
    const html = `
        <div class="list-item bg-light border p-3 mb-4 rounded-3">
            <div class="d-flex align-items-center mb-3">
                <i class="bi bi-grip-vertical drag-handle fs-4 me-2"></i>
                <input type="hidden" name="companies[${idx}].sortOrder" class="sort-order" value="99">
                <div class="flex-grow-1 me-3">
                    <input type="text" name="companies[${idx}].name" class="form-control fw-bold fs-5" placeholder="새로운 회사명">
                </div>
                <div class="mx-3 border-start ps-3 d-flex align-items-center">
                    <span class="badge bg-secondary me-2">TYPE</span>
                    <div class="form-check form-check-inline mb-0">
                        <input class="form-check-input" type="radio" name="companies[${idx}].type" value="WORK" checked id="compTypeWork${idx}">
                        <label class="form-check-label small" for="compTypeWork${idx}">WORK</label>
                    </div>
                    <div class="form-check form-check-inline mb-0">
                        <input class="form-check-input" type="radio" name="companies[${idx}].type" value="PERSONAL" id="compTypePersonal${idx}">
                        <label class="form-check-label small" for="compTypePersonal${idx}">PERSONAL</label>
                    </div>
                </div>
                <div class="form-check form-switch" title="노출 여부">
                     <input class="form-check-input" type="checkbox" checked onchange="this.nextElementSibling.value = this.checked">
                     <input type="hidden" name="companies[${idx}].visible" value="true">
                </div>
                <button type="button" class="btn btn-outline-danger btn-sm ms-3" onclick="removeElement(this)">삭제</button>
            </div>
            <div id="projectList-${idx}" class="project-sortable-list bg-white p-3 rounded border"></div>
            <div class="text-end mt-3">
                <button type="button" class="btn btn-primary btn-sm" onclick="addFullProject(${idx})">+ 프로젝트 추가</button>
            </div>
        </div>`;
    document.getElementById('companyList').insertAdjacentHTML('beforeend', html);
    initAllSortables();
}

function addEducation() {
    const idx = getSafeIndex('educationList', 'educations');
    const html = `
        <div class="list-item bg-light border p-3 mb-3 rounded position-relative">
            <div class="d-flex align-items-center mb-2">
                <i class="bi bi-grip-vertical drag-handle me-2 fs-5"></i>
                <input type="hidden" name="educations[${idx}].sortOrder" class="sort-order" value="99">
                <h6 class="mb-0 fw-bold">학력 정보</h6>
                <div class="ms-auto d-flex align-items-center">
                    <div class="form-check form-switch me-2">
                        <input class="form-check-input" type="checkbox" checked onchange="this.nextElementSibling.value = this.checked">
                        <input type="hidden" name="educations[${idx}].visible" value="true">
                    </div>
                    <button type="button" class="btn btn-sm btn-outline-danger" onclick="removeElement(this)">삭제</button>
                </div>
            </div>
            <div class="row g-2">
                <div class="col-md-6"><label class="form-label small text-muted mb-0">학교명</label><input type="text" name="educations[${idx}].institution" class="form-control form-control-sm" placeholder="예: 한국대학교"></div>
                <div class="col-md-3"><label class="form-label small text-muted mb-0">전공</label><input type="text" name="educations[${idx}].major" class="form-control form-control-sm" placeholder="예: 컴퓨터공학"></div>
                <div class="col-md-3"><label class="form-label small text-muted mb-0">학점</label><input type="text" name="educations[${idx}].gpa" class="form-control form-control-sm" placeholder="예: 4.0/4.5"></div>
                <div class="col-12"><label class="form-label small text-muted mb-0">기간</label><input type="text" name="educations[${idx}].period" class="form-control form-control-sm" placeholder="예: 2015.03 - 2022.02"></div>
                <div class="col-12"><label class="form-label small text-muted mb-0">추가 정보 (선택)</label><textarea name="educations[${idx}].additionalInfo" class="form-control form-control-sm" rows="2" placeholder="졸업 논문 주제 등"></textarea></div>
            </div>
        </div>`;
    document.getElementById('educationList').insertAdjacentHTML('beforeend', html);
}

function addCertification() {
    const idx = getSafeIndex('certificationList', 'certifications');
    const html = `
        <div class="list-item bg-light border p-3 mb-2 rounded position-relative">
            <div class="d-flex align-items-center mb-2">
                <i class="bi bi-grip-vertical drag-handle me-2 fs-5"></i>
                <input type="hidden" name="certifications[${idx}].sortOrder" class="sort-order" value="99">
                <h6 class="mb-0 fw-bold">자격증 정보</h6>
                <div class="ms-auto d-flex align-items-center">
                    <div class="form-check form-switch me-2">
                        <input class="form-check-input" type="checkbox" checked onchange="this.nextElementSibling.value = this.checked">
                        <input type="hidden" name="certifications[${idx}].visible" value="true">
                    </div>
                    <button type="button" class="btn btn-sm btn-outline-danger" onclick="removeElement(this)">삭제</button>
                </div>
            </div>
            <div class="row g-2">
                <div class="col-md-8"><label class="form-label small text-muted mb-0">자격증명</label><input type="text" name="certifications[${idx}].name" class="form-control form-control-sm" placeholder="예: 정보처리기사"></div>
                <div class="col-md-4"><label class="form-label small text-muted mb-0">취득일</label><input type="text" name="certifications[${idx}].issueDate" class="form-control form-control-sm" placeholder="예: 2023.05"></div>
                <div class="col-12"><label class="form-label small text-muted mb-0">추가 정보 (선택)</label><textarea name="certifications[${idx}].additionalInfo" class="form-control form-control-sm" rows="1" placeholder="발급기관 등"></textarea></div>
            </div>
        </div>`;
    document.getElementById('certificationList').insertAdjacentHTML('beforeend', html);
}

function addFullProject(cIdx) {
    const typeRadio = document.querySelector(`input[name="companyType-${cIdx}"]:checked`);
    const pType = typeRadio ? typeRadio.value : 'WORK';
    const pIdx = getSafeIndex(`projectList-${cIdx}`, `companies[${cIdx}].projects`);
    const m0=0, m1=1, m2=2, m3=3;

    const html = `
    <div class="project-item border-bottom pb-4 mb-4">
        <div class="d-flex align-items-center mb-2 bg-light p-2 rounded">
            <i class="bi bi-grip-vertical drag-handle me-2"></i>
            <span class="badge bg-warning text-dark me-2">NEW</span>
            <input type="hidden" name="companies[${cIdx}].projects[${pIdx}].sortOrder" class="project-sort-order" value="99">
            <input type="hidden" name="companies[${cIdx}].projects[${pIdx}].type" value="${pType}">
            <input type="text" name="companies[${cIdx}].projects[${pIdx}].title" class="form-control fw-bold border-0 bg-transparent" placeholder="새 프로젝트명">
            <div class="form-check form-switch ms-2"><input class="form-check-input" type="checkbox" checked onchange="this.nextElementSibling.value=this.checked"><input type="hidden" name="companies[${cIdx}].projects[${pIdx}].visible" value="true"></div>
            <button type="button" class="btn btn-sm btn-outline-danger ms-2" onclick="removeElement(this)">삭제</button>
        </div>
        <div id="metaList-${cIdx}-${pIdx}" class="meta-sortable-list ps-3 border-start ms-2">
            <div class="meta-item mb-3 bg-white border rounded p-2"><input type="hidden" name="companies[${cIdx}].projects[${pIdx}].metaItems[${m0}].itemType" value="DURATION"><input type="hidden" name="companies[${cIdx}].projects[${pIdx}].metaItems[${m0}].sortOrder" class="meta-sort-order" value="0"><div class="d-flex align-items-center mb-2 border-bottom pb-1"><i class="bi bi-grip-horizontal drag-handle me-2"></i><span class="badge bg-light text-dark border me-auto">📅 기간</span><div class="form-check form-switch form-check-sm"><input class="form-check-input" type="checkbox" checked onchange="this.nextElementSibling.value=this.checked"><input type="hidden" name="companies[${cIdx}].projects[${pIdx}].metaItems[${m0}].visible" value="true"></div></div><input type="text" name="companies[${cIdx}].projects[${pIdx}].metaItems[${m0}].content" class="form-control form-control-sm border-0" placeholder="기간"></div>
            <div class="meta-item mb-3 bg-white border rounded p-2"><input type="hidden" name="companies[${cIdx}].projects[${pIdx}].metaItems[${m1}].itemType" value="SUMMARY"><input type="hidden" name="companies[${cIdx}].projects[${pIdx}].metaItems[${m1}].sortOrder" class="meta-sort-order" value="1"><div class="d-flex align-items-center mb-2 border-bottom pb-1"><i class="bi bi-grip-horizontal drag-handle me-2"></i><span class="badge bg-light text-dark border me-auto">📝 요약</span><div class="form-check form-switch form-check-sm"><input class="form-check-input" type="checkbox" checked onchange="this.nextElementSibling.value=this.checked"><input type="hidden" name="companies[${cIdx}].projects[${pIdx}].metaItems[${m1}].visible" value="true"></div></div><textarea name="companies[${cIdx}].projects[${pIdx}].metaItems[${m1}].content" class="form-control form-control-sm border-0" rows="2" placeholder="요약"></textarea></div>
            <div class="meta-item mb-3 bg-white border rounded p-2"><input type="hidden" name="companies[${cIdx}].projects[${pIdx}].metaItems[${m2}].itemType" value="TECH_STACK_GROUP"><input type="hidden" name="companies[${cIdx}].projects[${pIdx}].metaItems[${m2}].sortOrder" class="meta-sort-order" value="2"><div class="d-flex align-items-center mb-2 border-bottom pb-1"><i class="bi bi-grip-horizontal drag-handle me-2"></i><span class="badge bg-light text-dark border me-auto">🛠 기술 스택</span><div class="form-check form-switch form-check-sm"><input class="form-check-input" type="checkbox" checked onchange="this.nextElementSibling.value=this.checked"><input type="hidden" name="companies[${cIdx}].projects[${pIdx}].metaItems[${m2}].visible" value="true"></div></div><div id="techList-${cIdx}-${pIdx}-${m2}" class="tech-sortable-list d-flex flex-wrap gap-2"></div><button type="button" class="btn btn-sm btn-link p-0 mt-2" onclick="addTechStack(${cIdx}, ${pIdx}, ${m2})">+ 기술</button></div>
            <div class="meta-item mb-3 bg-white border rounded p-2"><input type="hidden" name="companies[${cIdx}].projects[${pIdx}].metaItems[${m3}].itemType" value="CONTENT_GROUP"><input type="hidden" name="companies[${cIdx}].projects[${pIdx}].metaItems[${m3}].sortOrder" class="meta-sort-order" value="3"><div class="d-flex align-items-center mb-2 border-bottom pb-1"><i class="bi bi-grip-horizontal drag-handle me-2"></i><span class="badge bg-light text-dark border me-auto">상세 내용</span><div class="form-check form-switch form-check-sm"><input class="form-check-input" type="checkbox" checked onchange="this.nextElementSibling.value=this.checked"><input type="hidden" name="companies[${cIdx}].projects[${pIdx}].metaItems[${m3}].visible" value="true"></div></div><div id="probList-${cIdx}-${pIdx}-${m3}"></div><button type="button" class="btn btn-sm btn-outline-secondary w-100 mt-2" onclick="addProblem(${cIdx}, ${pIdx}, ${m3})">+ 에피소드</button></div>
        </div>
    </div>`;
    document.getElementById(`projectList-${cIdx}`).insertAdjacentHTML('beforeend', html);
    initAllSortables();
    addTechStack(cIdx, pIdx, m2);
    addProblem(cIdx, pIdx, m3);
}

function addTechStack(c,p,m) {
    const id = `techList-${c}-${p}-${m}`;
    const tIdx = getSafeIndex(id, `companies[${c}].projects[${p}].metaItems[${m}].techStacks`);
    const html = `<div class="tech-item input-group input-group-sm w-auto"><span class="input-group-text bg-light border-0 px-1 drag-handle">::</span><input type="hidden" name="companies[${c}].projects[${p}].metaItems[${m}].techStacks[${tIdx}].sortOrder" class="tech-sort-order" value="99"><input type="text" name="companies[${c}].projects[${p}].metaItems[${m}].techStacks[${tIdx}].techName" class="form-control" placeholder="기술명"><div class="input-group-text bg-white border-start-0 ps-1"><input class="form-check-input mt-0" type="checkbox" checked onchange="this.nextElementSibling.value=this.checked; syncAllSkills();"><input type="hidden" name="companies[${c}].projects[${p}].metaItems[${m}].techStacks[${tIdx}].visible" value="true"></div><button type="button" class="btn btn-outline-secondary" onclick="removeElement(this)">x</button></div>`;
    document.getElementById(id).insertAdjacentHTML('beforeend', html);
    initAllSortables();
}

function addProblem(c,p,m) {
    const id = `probList-${c}-${p}-${m}`;
    const pbIdx = getSafeIndex(id, `companies[${c}].projects[${p}].metaItems[${m}].problems`);
    const html = `
        <div class="mb-3 ps-3 border-start border-3 border-light episode-item">
            <div class="mb-2 d-flex align-items-center">
                <input type="hidden" name="companies[${c}].projects[${p}].metaItems[${m}].problems[${pbIdx}].sortOrder" value="0">
                <input type="text" name="companies[${c}].projects[${p}].metaItems[${m}].problems[${pbIdx}].title" class="form-control form-control-sm fw-bold bg-light" placeholder="문제 정의 (Problem)">
                <button type="button" class="btn btn-xs btn-link text-danger ms-2 text-decoration-none" onclick="removeElement(this)">삭제</button>
            </div>
            <div id="solList-${c}-${p}-${m}-${pbIdx}" class="solution-sortable-list ps-3 mb-1"></div>
            <div class="ps-4 mb-2"><button type="button" class="btn btn-xs btn-light text-primary py-0" style="font-size:0.7rem" onclick="addSolution(${c}, ${p}, ${m}, ${pbIdx})">+ 해결책</button></div>
            <div id="impList-${c}-${p}-${m}-${pbIdx}" class="impact-sortable-list ps-3"></div>
            <div class="ps-4"><button type="button" class="btn btn-xs btn-light text-success py-0" style="font-size:0.7rem" onclick="addImpact(${c}, ${p}, ${m}, ${pbIdx})">+ 성과</button></div>
        </div>`;
    document.getElementById(id).insertAdjacentHTML('beforeend', html);
    addSolution(c, p, m, pbIdx);
    addImpact(c, p, m, pbIdx);
    if(typeof initAllSortables === 'function') initAllSortables();
}

function addSolution(c,p,m,pb) {
    const id = `solList-${c}-${p}-${m}-${pb}`;
    const idx = getSafeIndex(id, `companies[${c}].projects[${p}].metaItems[${m}].problems[${pb}].solutions`);
    const html = `<div class="solution-item input-group input-group-sm mb-1"><span class="input-group-text bg-white border-0 px-1 drag-handle" style="cursor:move;">::</span><input type="hidden" name="companies[${c}].projects[${p}].metaItems[${m}].problems[${pb}].solutions[${idx}].sortOrder" class="solution-sort-order" value="99"><input type="text" name="companies[${c}].projects[${p}].metaItems[${m}].problems[${pb}].solutions[${idx}].content" class="form-control" placeholder="해결책"><div class="input-group-text bg-white border-start-0 ps-1"><input class="form-check-input mt-0" type="checkbox" checked onchange="this.nextElementSibling.value=this.checked"><input type="hidden" name="companies[${c}].projects[${p}].metaItems[${m}].problems[${pb}].solutions[${idx}].visible" value="true"></div><button type="button" class="btn btn-outline-light text-danger border-0" onclick="removeElement(this)">x</button></div>`;
    document.getElementById(id).insertAdjacentHTML('beforeend', html);
    initAllSortables();
}

function addImpact(c,p,m,pb) {
    const id = `impList-${c}-${p}-${m}-${pb}`;
    const idx = getSafeIndex(id, `companies[${c}].projects[${p}].metaItems[${m}].problems[${pb}].impacts`);
    const html = `<div class="impact-item input-group input-group-sm mb-1"><span class="input-group-text bg-white border-0 px-1 drag-handle" style="cursor:move;">::</span><input type="hidden" name="companies[${c}].projects[${p}].metaItems[${m}].problems[${pb}].impacts[${idx}].sortOrder" class="impact-sort-order" value="99"><input type="text" name="companies[${c}].projects[${p}].metaItems[${m}].problems[${pb}].impacts[${idx}].content" class="form-control" placeholder="성과"><div class="input-group-text bg-white border-start-0 ps-1"><input class="form-check-input mt-0" type="checkbox" checked onchange="this.nextElementSibling.value=this.checked"><input type="hidden" name="companies[${c}].projects[${p}].metaItems[${m}].problems[${pb}].impacts[${idx}].visible" value="true"></div><button type="button" class="btn btn-outline-light text-danger border-0" onclick="removeElement(this)">x</button></div>`;
    document.getElementById(id).insertAdjacentHTML('beforeend', html);
    initAllSortables();
}