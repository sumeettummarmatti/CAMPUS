package com.campus.payment.service;

import com.campus.payment.model.Transaction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class PaymentUserSyncService {

    private final RestTemplate restTemplate;

    @Value("${services.user-service-url}")
    private String userServiceUrl;

    public PaymentUserSyncService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean isModeEnabledForUser(Long userId, String mode) {
        List<String> enabledModes = getEnabledModesForUser(userId);
        return enabledModes.contains(mode);
    }

    public List<String> getEnabledModesForUser(Long userId) {
        String url = userServiceUrl + "/api/users/internal/" + userId + "/wallet/modes";
        ResponseEntity<List<String>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                }
        );
        List<String> enabledModes = response.getBody();
        return enabledModes == null ? List.of() : enabledModes;
    }

    public void syncTransaction(Transaction tx) {
        String url = userServiceUrl + "/api/users/internal/transactions/sync";
        Map<String, Object> payload = Map.of(
                "transactionId", tx.getId(),
                "auctionId", tx.getAuctionId(),
                "winnerId", tx.getWinnerId(),
                "sellerId", tx.getSellerId(),
                "amount", tx.getAmount(),
                "status", tx.getStatus().name(),
                "paymentMethod", tx.getPaymentMethod().name()
        );
        restTemplate.postForEntity(url, new HttpEntity<>(payload), Void.class);
    }
}
