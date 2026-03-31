package com.campus.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO for opening a payment dispute.
 */
@Data
public class DisputeRequest {

    @NotBlank(message = "Dispute reason is required")
    private String reason;
}
