package com.example.profile.dto;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor
public class OrderDTO {

    @Id
    @Column(name = "order_id")
    private String orderId; // 토스페이먼츠는 고유한 문자열 ID를 요구

    private String orderName;
    private Long amount;
    private String status;

    public OrderDTO(String orderId, String orderName, Long amount) {
        this.orderId = orderId;
        this.orderName = orderName;
        this.amount = amount;
        this.status = "PENDING"; // 초기 상태
    }

    public void completePayment() {
        this.status = "SUCCESS";
    }
}
