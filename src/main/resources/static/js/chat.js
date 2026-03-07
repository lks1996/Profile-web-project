// 1. 대화 상태(State) 관리 배열.
let chatHistory = [];

// 2. 엔터키 입력 처리 함수.
function handleEnter(event) {
    if (event.key === 'Enter') {
        sendMessage();
    }
}

// 3. 핵심 통신 및 렌더링 함수.
async function sendMessage() {
    const inputElement = document.getElementById('chat-input');
    const userMessage = inputElement.value.trim();

    // 빈 메시지 전송 방어 로직.
    if (!userMessage) return;

    // 화면에 사용자 메시지 즉시 렌더링 및 입력창 초기화.
    appendMessageToUI("user", userMessage);
    inputElement.value = '';

    // 서버로 보낼 DTO 조립.
    const payloadWindow = chatHistory.slice(-6);
    const requestDto = {
        history: payloadWindow,
        newMessage: userMessage
    };

    // AI가 답변을 타이핑할 빈 말풍선을 미리 생성.
    const aiMessageBoxText = appendMessageToUI("model", "");

    try {
        // 백엔드로 POST 스트리밍 요청 발송.
        const response = await fetch('/api/chat/stream', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestDto)
        });

        // 429 에러(트래픽 초과) 발생 시 인터셉터의 메시지를 직접 화면에 출력. (방어 로직 복구)
        if (response.status === 429) {
            const errorMessage = await response.text();
            aiMessageBoxText.innerHTML = errorMessage;
            return;
        }

        if (!response.ok) {
            throw new Error(`서버 응답 에러: ${response.status}`);
        }

        // 스트림 데이터 조각(Chunk)을 UTF-8로 번역하며 읽기.
        const reader = response.body.getReader();
        const decoder = new TextDecoder("utf-8");

        let aiFullResponse = "";
        let buffer = "";

        while (true) {
            const { done, value } = await reader.read();
            if (done) break;

            // 1. 새로 도착한 바이트를 텍스트로 변환 후 버퍼에 계속 이어 붙임.
            buffer += decoder.decode(value, { stream: true });

            // 2. 줄바꿈 기호(\n)를 기준으로 버퍼의 데이터 분힐.
            const lines = buffer.split('\n');

            // 3. 배열의 맨 마지막 요소는 아직 줄바꿈이 끝나지 않은 '불완전한 문장'이므로 다시 버퍼에 넣음.
            buffer = lines.pop();

            // 4. 완벽하게 줄바꿈이 끝난 온전한 문장만 파싱.
            for (const line of lines) {
                const trimmedLine = line.trim();
                if (trimmedLine.startsWith('data:')) {
                    // "data:" 글자를 떼어내고 순수 JSON 문자열만 추출
                    const jsonString = trimmedLine.substring(5).trim();

                    if (jsonString) {
                        try {
                            // 문자열을 자바스크립트 객체(JSON)로 변환.
                            const parsedData = JSON.parse(jsonString);

                            // JSON 구조 안에서 실제 텍스트 데이터만 추출.
                            if (parsedData.candidates && parsedData.candidates[0].content && parsedData.candidates[0].content.parts) {
                                const actualText = parsedData.candidates[0].content.parts[0].text;
                                aiFullResponse += actualText;
                            }
                        } catch (error) {
                            // 버퍼 조립 전 발생하는 사소한 파싱 에러는 무시하고 스트림을 계속 유지.
                            console.error("JSON 파싱 에러 (조립 대기 중):", error);
                        }
                    }
                }
            }

            // 5. 알맹이만 모인 텍스트를 화면에 렌더링
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

// 스크롤 하단 고정 함수
function scrollToBottom() {
    const chatContainer = document.getElementById('chat-messages-container');
    chatContainer.scrollTop = chatContainer.scrollHeight;
}