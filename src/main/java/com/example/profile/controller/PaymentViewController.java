package com.example.profile.controller;

import com.example.profile.model.PaymentConfirmRequest;
import com.example.profile.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@Slf4j
public class PaymentViewController {

    private final PaymentService paymentService;

    // 토스페이먼츠에서 결제 인증 성공 후 리다이렉트 되는 주소
    @GetMapping("/payment/success")
    public String paymentSuccess(
            @RequestParam String paymentKey,
            @RequestParam String orderId,
            @RequestParam Long amount,
            Model model) {

        try {
            // 1. 전달받은 파라미터로 DTO 생성 (이전 단계에서 만든 클래스 재사용)
            PaymentConfirmRequest request = new PaymentConfirmRequest(paymentKey, orderId, amount);

            // 2. 서비스 단의 최종 승인 로직 호출 (금액 검증 및 토스 API 통신)
            paymentService.confirmTossPayment(request);

            // 3. 최종 승인까지 성공하면 모델에 메시지를 담아 성공 페이지 렌더링
            log.warn("Somebody sent coffee for you!");
            model.addAttribute("message", "커피 선물 감사합니다! 결제가 성공적으로 완료되었습니다.");
            return "payment/success";

        } catch (Exception e) {
            // 조작된 금액이거나 통신 오류가 발생하면 실패 페이지로 이동
            model.addAttribute("message", "결제 승인 중 오류가 발생했습니다: " + e.getMessage());
            return "payment/fail";
        }
    }

    // 결제창에서 취소하거나 인증 실패 시 리다이렉트 되는 주소
    @GetMapping("/payment/fail")
    public String paymentFail(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String message,
            Model model) {

        model.addAttribute("message", message != null ? message : "결제가 취소되었거나 실패했습니다.");
        return "payment/fail";
    }
}
