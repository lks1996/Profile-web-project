package com.example.profile.controller;

import com.example.profile.dto.OrderDTO;
import com.example.profile.model.PaymentConfirmRequest;
import com.example.profile.repository.OrderRepository;
import com.example.profile.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderRepository orderRepository;

    @PostMapping("/order")
    public ResponseEntity<OrderDTO> createOrder() {
        // 고유 주문번호(UUID) 생성.
        String newOrderId = UUID.randomUUID().toString();

        OrderDTO newOrder = new OrderDTO(newOrderId, "격려의 커피 한 잔", 5000L);

        // DB에 PENDING(대기) 상태로 저장.
        orderRepository.save(newOrder);

        // 생성된 주문 정보 반환.
        return ResponseEntity.ok(newOrder);
    }

    @PostMapping("/confirm")
    public ResponseEntity<String> confirmPayment(@RequestBody PaymentConfirmRequest request) {
        try {
            paymentService.confirmTossPayment(request);
            return ResponseEntity.ok("결제가 성공적으로 완료되었습니다!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("결제 승인 실패: " + e.getMessage());
        }
    }
}