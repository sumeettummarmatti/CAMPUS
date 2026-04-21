package com.campus.user.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class TopUpRequest {

    @NotNull
    @DecimalMin(value = "1.00", message = "Minimum top-up amount is ₹1.00")
    private BigDecimal amount;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
