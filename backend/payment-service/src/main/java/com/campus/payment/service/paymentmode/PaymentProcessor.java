package com.campus.payment.service.paymentmode;

import com.campus.payment.dto.PaymentRequest;
import com.campus.payment.model.PaymentMethod;

/**
 * Strategy contract for processing a specific payment mode.
 */
public interface PaymentProcessor {
    PaymentMethod supportedMethod();
    boolean charge(PaymentRequest request);
}
