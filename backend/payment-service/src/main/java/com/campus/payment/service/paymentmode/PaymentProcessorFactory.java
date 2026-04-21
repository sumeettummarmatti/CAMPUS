package com.campus.payment.service.paymentmode;

import com.campus.payment.model.PaymentMethod;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Manual Factory Pattern for selecting payment processors by mode.
 */
@Component
public class PaymentProcessorFactory {

    private final Map<PaymentMethod, PaymentProcessor> processorMap = new EnumMap<>(PaymentMethod.class);

    public PaymentProcessorFactory(List<PaymentProcessor> processors) {
        for (PaymentProcessor processor : processors) {
            processorMap.put(processor.supportedMethod(), processor);
        }
    }

    public PaymentProcessor create(PaymentMethod method) {
        PaymentProcessor processor = processorMap.get(method);
        if (processor == null) {
            throw new IllegalArgumentException("Unsupported payment method: " + method);
        }
        return processor;
    }
}
