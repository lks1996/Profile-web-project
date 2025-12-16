// resume.js

document.addEventListener('DOMContentLoaded', () => {
    fetch('/api/v1/resume')
        .then(response => response.json())
        .then(data => {
            renderHeader(data);
            renderSections(data.sections);
        })
        .catch(error => console.error('Error fetching resume data:', error));
});

function renderHeader(data) {
    const headerEl = document.getElementById('header');
    headerEl.innerHTML = `
        <h1>${data.fullName}</h1>
        <p>${data.jobTitle} | ${data.email} | ${data.github}</p>
    `;
}

function renderSections(sections) {
    const contentEl = document.getElementById('resume-content');

    sections.forEach(section => {
        const sectionDiv = document.createElement('div');
        sectionDiv.className = 'section-container';

        // 1. 섹션 헤딩 출력
        sectionDiv.innerHTML = `<h2>${section.sectionName.toUpperCase()}</h2>`;

        // 2. 섹션 내용 렌더링 (switch/case로 각 섹션 타입을 처리)
        switch (section.sectionName) {
            case 'ABOUT':
                renderAbout(section.content, sectionDiv);
                break;
            case 'PROJECTS':
                renderProjects(section.content, sectionDiv);
                break;
            case 'SKILLS':
                // 스킬 데이터 렌더링 로직...
                break;
        }
        contentEl.appendChild(sectionDiv);
    });
}

// 예시: PROJECTS 섹션 렌더링 로직 (ProjectGroupDTO List 처리)
function renderProjects(projectGroups, parentEl) {
    projectGroups.forEach(group => {
        // 회사/그룹 헤더 (예: [리플럭스] 또는 [Personal Projects])
        parentEl.innerHTML += `<h3>${group.groupName}</h3>`;

        group.projects.forEach(project => {
            const projectEl = document.createElement('div');
            projectEl.className = 'project-item';
            projectEl.innerHTML = `<h4>${project.title}</h4>`;

            // ProjectItemDTOs (기간, 스택, 내용)을 순서대로 출력
            project.projectItems.forEach(item => {
                // ... item.itemType에 따라 HTML 요소 생성 로직 추가 ...
                // 예: item.itemType이 'DURATION'이면 기간 출력, 'CONTENT_GROUP'이면 Problem/Solution/Impact 출력
            });
            parentEl.appendChild(projectEl);
        });
    });
}