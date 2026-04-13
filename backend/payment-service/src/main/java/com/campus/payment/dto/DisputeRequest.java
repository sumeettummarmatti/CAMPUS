package com.campus.payment.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for opening a payment dispute.
 */
public class DisputeRequest {

    @NotBlank(message = "Dispute reason is required")
    private String reason;

    public DisputeRequest() {}

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
