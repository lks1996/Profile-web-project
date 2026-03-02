package com.example.profile.service;

import com.example.profile.dto.OrderDTO;
import com.example.profile.model.PaymentConfirmRequest;
import com.example.profile.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${api.payments.toss.secret-key}")
    private String SECRET_KEY;

    @Transactional
    public void confirmTossPayment(PaymentConfirmRequest request) {
        // 1. DB에서 주문 조회.
        OrderDTO order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        // 2. 금액 조작 검증 (백엔드의 핵심 방어 로직)
        if (!order.getAmount().equals(request.getAmount())) {
            throw new IllegalArgumentException("결제 금액이 위변조되었습니다.");
        }

        // 3. 토스페이먼츠 API로 최종 승인 요청
        HttpHeaders headers = new HttpHeaders();
        // 시크릿 키 뒤에 콜론(:)을 붙여서 Base64로 인코딩.
        String encodedAuth = Base64.getEncoder().encodeToString((SECRET_KEY + ":").getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encodedAuth);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> params = new HashMap<>();
        params.put("paymentKey", request.getPaymentKey());
        params.put("orderId", request.getOrderId());
        params.put("amount", request.getAmount());

        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(params, headers);

        // API 통신
        ResponseEntity<String> response = restTemplate.postForEntity(
                "https://api.tosspayments.com/v1/payments/confirm",
                httpEntity,
                String.class
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            // 4. 결제 성공 시 DB 상태 업데이트.
            order.completePayment();
        } else {
            throw new RuntimeException("토스 결제 승인 API 호출 실패");
        }
    }
}