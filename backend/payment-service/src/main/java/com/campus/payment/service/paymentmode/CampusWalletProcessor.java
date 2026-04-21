package com.campus.payment.service.paymentmode;

import com.campus.payment.dto.PaymentRequest;
import com.campus.payment.model.PaymentMethod;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CampusWalletProcessor implements PaymentProcessor {

    @Override
    public PaymentMethod supportedMethod() {
        return PaymentMethod.CAMPUS_WALLET;
    }

    @Override
    public boolean charge(PaymentRequest request) {
        return request.getAmount().compareTo(BigDecimal.ZERO) > 0;
    }
}
