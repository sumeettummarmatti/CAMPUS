package com.campus.payment.controller;

import com.campus.payment.dto.PaymentDTO;
import com.campus.payment.dto.PaymentRequest;
import com.campus.payment.service.EscrowService;
import com.campus.payment.service.PaymentGatewayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for payment lifecycle and escrow operations (MVC: Controller).
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentGatewayService paymentGatewayService;
    private final EscrowService escrowService;

    /**
     * POST /api/payments/initiate — create a new PENDING payment for a completed auction.
     */
    @PostMapping("/initiate")
    public ResponseEntity<PaymentDTO> initiate(@Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.status(201).body(paymentGatewayService.initiatePayment(request));
    }

    /**
     * POST /api/payments/{id}/confirm — confirm payment, moves PENDING → IN_ESCROW.
     */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<PaymentDTO> confirm(@PathVariable Long id) {
        return ResponseEntity.ok(paymentGatewayService.confirmPayment(id));
    }

    /**
     * POST /api/payments/{id}/cancel — cancel a PENDING payment.
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<PaymentDTO> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(paymentGatewayService.cancelPayment(id));
    }

    /**
     * GET /api/payments/{id} — retrieve a transaction by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PaymentDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentGatewayService.getTransaction(id));
    }

    /**
     * GET /api/payments/auction/{auctionId} — all transactions for an auction.
     */
    @GetMapping("/auction/{auctionId}")
    public ResponseEntity<List<PaymentDTO>> byAuction(@PathVariable Long auctionId) {
        return ResponseEntity.ok(paymentGatewayService.getTransactionsByAuction(auctionId));
    }

    /**
     * GET /api/payments/winner/{winnerId} — all transactions where user is the buyer.
     */
    @GetMapping("/winner/{winnerId}")
    public ResponseEntity<List<PaymentDTO>> byWinner(@PathVariable Long winnerId) {
        return ResponseEntity.ok(paymentGatewayService.getTransactionsByWinner(winnerId));
    }

    /**
     * GET /api/payments/seller/{sellerId} — all transactions where user is the seller.
     */
    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<List<PaymentDTO>> bySeller(@PathVariable Long sellerId) {
        return ResponseEntity.ok(paymentGatewayService.getTransactionsBySeller(sellerId));
    }

    // ── Escrow endpoints ────────────────────────────────────────────────────

    /**
     * POST /api/payments/{id}/escrow/hold — hold funds in escrow (PENDING → IN_ESCROW).
     */
    @PostMapping("/{id}/escrow/hold")
    public ResponseEntity<PaymentDTO> hold(@PathVariable Long id) {
        return ResponseEntity.ok(escrowService.holdFunds(id));
    }

    /**
     * POST /api/payments/{id}/escrow/release — release escrowed funds to seller.
     */
    @PostMapping("/{id}/escrow/release")
    public ResponseEntity<PaymentDTO> release(@PathVariable Long id) {
        return ResponseEntity.ok(escrowService.releaseFunds(id));
    }

    /**
     * POST /api/payments/{id}/escrow/refund — refund escrowed funds to buyer.
     */
    @PostMapping("/{id}/escrow/refund")
    public ResponseEntity<PaymentDTO> refund(@PathVariable Long id) {
        return ResponseEntity.ok(escrowService.refundFunds(id));
    }
}
