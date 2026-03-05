// 1. 대화 상태(State) 관리 배열
let chatHistory = [];

// 2. 엔터키 입력 처리 함수
function handleEnter(event) {
    if (event.key === 'Enter') {
        sendMessage();
    }
}

// 3. 핵심 통신 및 렌더링 함수
async function sendMessage() {
    const inputElement = document.getElementById('chat-input');
    const userMessage = inputElement.value.trim();

    // 빈 메시지 전송 방어 로직
    if (!userMessage) return;

    // 화면에 사용자 메시지 즉시 렌더링 및 입력창 초기화
    appendMessageToUI("user", userMessage);
    inputElement.value = '';

    // 서버로 보낼 DTO 조립 (최근 6개 대화만 자르는 슬라이딩 윈도우 적용)
    const payloadWindow = chatHistory.slice(-6);
    const requestDto = {
        history: payloadWindow,
        newMessage: userMessage
    };

    // AI가 답변을 타이핑할 빈 말풍선을 미리 생성
    const aiMessageBoxText = appendMessageToUI("model", "");

    try {
        // 백엔드로 POST 스트리밍 요청 발송
        const response = await fetch('/api/chat/stream', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestDto)
        });

        if (!response.ok) {
            throw new Error(`서버 응답 에러: ${response.status}`);
        }

        // 스트림 데이터 조각(Chunk)을 UTF-8로 번역하며 읽기
        const reader = response.body.getReader();
        const decoder = new TextDecoder("utf-8");
        let aiFullResponse = "";

        while (true) {
            const { done, value } = await reader.read();
            if (done) break;

            // 바이트를 글자로 디코딩
            const chunkText = decoder.decode(value, { stream: true });
            aiFullResponse += chunkText;

            // 화면의 말풍선에 글자를 누적하여 실시간 타이핑 효과 구현
            // 줄바꿈 문자(\n)를 HTML 태그(<br>)로 치환하여 렌더링
            aiMessageBoxText.innerHTML = aiFullResponse.replace(/\n/g, '<br>');

            // 스크롤을 항상 맨 아래로 유지
            scrollToBottom();
        }

        // 통신이 성공적으로 끝나면, 이번 대화 턴(Turn)을 기억 배열에 저장
        chatHistory.push({ role: "user", text: userMessage });
        chatHistory.push({ role: "model", text: aiFullResponse });

    } catch (error) {
        console.error("챗봇 통신 에러:", error);
        aiMessageBoxText.innerHTML = "죄송합니다, 통신 중 오류가 발생했습니다. 서버 상태를 확인해주세요.";
    }
}

// UI 렌더링 헬퍼 함수
function appendMessageToUI(role, text) {
    const chatContainer = document.getElementById('chat-messages-container');

    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${role}`;

    const textDiv = document.createElement('div');
    textDiv.className = 'text';
    textDiv.innerHTML = text.replace(/\n/g, '<br>');

    messageDiv.appendChild(textDiv);
    chatContainer.appendChild(messageDiv);

    scrollToBottom();

    return textDiv; // 내용 업데이트를 위해 생성된 텍스트 노드 반환
}

// 스크롤 하단 고정 헬퍼 함수
function scrollToBottom() {
    const chatContainer = document.getElementById('chat-messages-container');
    chatContainer.scrollTop = chatContainer.scrollHeight;
}