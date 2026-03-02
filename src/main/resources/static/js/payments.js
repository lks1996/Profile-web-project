// 1. 테스트용 클라이언트 키 (프론트엔드 노출용)
const clientKey = "test_ck_Z1aOwX7K8m7BqBD5b4JjVyQxzvNP";
const tossPayments = TossPayments(clientKey);

document.getElementById("payment-button").addEventListener("click", async function () {

    try {
        // 2. 사바에 주문 사전 생성 요청.
        const response = await fetch('/api/payments/order', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error("주문 생성에 실패했습니다.");
        }

        // 3. 백엔드가 DB에 저장하고 반환한 주문정보 추출.
        const orderData = await response.json();

        // 4. 백엔드가 발급해준 orderId와 amount를 사용하여 결제창 호출
        tossPayments.requestPayment("카드", {
            amount: orderData.amount,
            orderId: orderData.orderId,
            orderName: orderData.orderName,
            successUrl: window.location.origin + "/payment/success",
            failUrl: window.location.origin + "/payment/fail",
        });

    } catch (error) {
        console.error(error);
        alert("결제 초기화 중 문제가 발생했습니다.");
    }
});