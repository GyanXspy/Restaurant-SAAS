package com.restaurant.order.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmOrderCommand {
    private String orderId;
    private String paymentId;
}
