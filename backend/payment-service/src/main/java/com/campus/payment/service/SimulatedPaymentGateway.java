package com.campus.payment.service;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class SimulatedPaymentGateway implements IPaymentGateway {
    
    @Override
    public boolean charge(BigDecimal amount, String paymentMethodRef) {
        // Replace this with Razorpay/UPI SDK calls for production
        return true;
    }
    
    @Override
    public boolean verify(String gatewayRef) {
        // Replace this with Razorpay/UPI SDK calls for production
        return true;
    }
    
    @Override
    public boolean refund(String gatewayRef, BigDecimal amount) {
        // Replace this with Razorpay/UPI SDK calls for production
        return true;
    }
}
