package com.campus.payment.controller;

import com.campus.payment.dto.DisputeRequest;
import com.campus.payment.dto.PaymentDTO;
import com.campus.payment.service.DisputeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for dispute management (MVC: Controller).
 */
@RestController
@RequestMapping("/api/disputes")
public class DisputeController {

    private final DisputeService disputeService;

    public DisputeController(DisputeService disputeService) {
        this.disputeService = disputeService;
    }

    /**
     * POST /api/disputes/{id}/open — open a dispute on an in-escrow payment.
     */
    @PostMapping("/{id}/open")
    public ResponseEntity<PaymentDTO> open(
            @PathVariable Long id,
            @Valid @RequestBody DisputeRequest request) {
        return ResponseEntity.ok(disputeService.openDispute(id, request.getReason()));
    }

    /**
     * POST /api/disputes/{id}/review — place dispute under admin review.
     */
    @PostMapping("/{id}/review")
    public ResponseEntity<PaymentDTO> review(@PathVariable Long id) {
        return ResponseEntity.ok(disputeService.reviewDispute(id));
    }

    /**
     * POST /api/disputes/{id}/resolve/buyer — resolve in buyer's favour (refund).
     */
    @PostMapping("/{id}/resolve/buyer")
    public ResponseEntity<PaymentDTO> resolveBuyer(@PathVariable Long id) {
        return ResponseEntity.ok(disputeService.resolveDisputeBuyer(id));
    }

    /**
     * POST /api/disputes/{id}/resolve/seller — resolve in seller's favour (release).
     */
    @PostMapping("/{id}/resolve/seller")
    public ResponseEntity<PaymentDTO> resolveSeller(@PathVariable Long id) {
        return ResponseEntity.ok(disputeService.resolveDisputeSeller(id));
    }

    /**
     * POST /api/disputes/{id}/close — close dispute without action (e.g. withdrawn).
     */
    @PostMapping("/{id}/close")
    public ResponseEntity<PaymentDTO> close(@PathVariable Long id) {
        return ResponseEntity.ok(disputeService.closeDispute(id));
    }
}
