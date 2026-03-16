package com.campus.payment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Simulated payment gateway integration.
 * In production, this would call Razorpay / Stripe / PayU APIs.
 */
@Service
@Slf4j
public class PaymentGatewayService {

    /**
     * Charge the buyer's payment method.
     * Returns a simulated transaction reference.
     */
    public String chargePayment(BigDecimal amount, Long buyerId) {
        log.info("GATEWAY: Charging ₹{} from buyer {}", amount, buyerId);
        // Simulate successful charge
        String txnRef = "PAY-" + System.currentTimeMillis();
        log.info("GATEWAY: Charge successful — ref: {}", txnRef);
        return txnRef;
    }

    /**
     * Refund a previous charge.
     */
    public boolean refundPayment(String paymentReference) {
        log.info("GATEWAY: Refunding payment {}", paymentReference);
        // Simulate successful refund
        return true;
    }
}
