document.addEventListener("DOMContentLoaded", function() {

    // 1. 관찰자(Observer) 옵션 설정
    const observerOptions = {
        root: null,         // 뷰포트 기준
        rootMargin: '0px',
        threshold: 0.1      // 요소가 10% 보일 때 트리거 (조금 더 빨리 뜨게 수정)
    };

    // 2. 관찰자 콜백 함수
    const observerCallback = (entries, observer) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                // 화면에 들어오면 .is-visible 클래스 추가
                entry.target.classList.add('is-visible');

                // (선택) 한 번 애니메이션 후 감시 해제하려면 주석 해제
                // observer.unobserve(entry.target);
            }
        });
    };

    // 3. 관찰자 생성
    const observer = new IntersectionObserver(observerCallback, observerOptions);

    // 4. 모든 타겟 요소(.reveal-on-scroll) 등록
    document.querySelectorAll('.reveal-on-scroll').forEach(el => observer.observe(el));
});


// [연락처 불러오기 AJAX]
// [연락처 불러오기 AJAX]
function loadContactInfo(btnElement) {
    const rawId = btnElement.getAttribute('data-id');

    if (!rawId) {
        alert("이력서 정보가 올바르지 않습니다.");
        return;
    }

    const profileId = parseInt(rawId, 10);
    const divider = document.getElementById('contactDivider');
    const noMsg = document.getElementById('noContactMsg');

    // 버튼 로딩 상태
    const originalText = btnElement.innerHTML;
    btnElement.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>';
    btnElement.disabled = true;

    fetch('/api/contact/reveal', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ id: profileId })
    })
        .then(response => {
            if (!response.ok) throw new Error('Network response was not ok');
            return response.json();
        })
        .then(data => {
            const phoneText = document.getElementById('phoneText');
            const phoneLink = document.getElementById('phoneLink');
            const emailText = document.getElementById('emailText');
            const emailLink = document.getElementById('emailLink');
            const infoDiv = document.getElementById('realContactInfo');

            // 데이터 존재 여부 체크
            const hasPhone = data.phone && data.phone.trim() !== "";
            const hasEmail = data.email && data.email.trim() !== "";

            // CASE 1: 데이터가 둘 다 없는 경우
            if (!hasPhone && !hasEmail) {
                btnElement.style.display = 'none';
                if(divider) divider.style.display = 'none';

                if(noMsg) {
                    noMsg.style.display = 'inline-block';
                } else {
                    alert("등록된 연락처 정보가 없습니다.");
                }
                return;
            }

            // CASE 2: 데이터가 있는 경우 (안전장치 추가)
            if (hasPhone) {
                // [수정] 요소가 존재하는지 확인 후 값 넣기
                if (phoneText) phoneText.innerText = data.phone;
                if (phoneLink) {
                    phoneLink.href = 'tel:' + data.phone;
                    phoneLink.style.display = 'flex';
                }
            } else {
                if (phoneLink) phoneLink.style.display = 'none';
            }

            if (hasEmail) {
                // [수정] 요소가 존재하는지 확인 후 값 넣기
                if (emailText) emailText.innerText = data.email;
                if (emailLink) {
                    emailLink.href = 'mailto:' + data.email;
                    emailLink.style.display = 'flex';
                }
            } else {
                if (emailLink) emailLink.style.display = 'none';
            }

            // 화면 전환
            btnElement.style.display = 'none';
            if(divider) divider.style.display = 'none';

            if(infoDiv) {
                infoDiv.style.display = 'flex';
                setTimeout(() => {
                    infoDiv.style.opacity = '1';
                }, 50);
            }
        })
        .catch(error => {
            console.error('Error:', error);
            btnElement.innerHTML = originalText;
            btnElement.disabled = false;
            alert('정보를 불러오는데 실패했습니다.');
        });
}