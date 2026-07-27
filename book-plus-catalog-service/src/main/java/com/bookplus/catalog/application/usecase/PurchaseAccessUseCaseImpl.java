package com.bookplus.catalog.application.usecase;

import com.bookplus.catalog.domain.model.Purchase;
import com.bookplus.catalog.domain.port.in.PurchaseAccessUseCase;
import com.bookplus.catalog.domain.port.out.PurchasePort;
import com.bookplus.catalog.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class PurchaseAccessUseCaseImpl implements PurchaseAccessUseCase {

    private final PurchasePort purchasePort;

    @Override
    public void grantAccess(String userId, String bookId) {
        if (purchasePort.find(userId, bookId).isEmpty()) {
            purchasePort.save(new Purchase(userId, bookId, Instant.now(), true, false, 0));
            log.info("Registered purchase: user={} book={}", userId, bookId);
        }
    }

    @Override
    public void revokeAccess(String userId, String bookId) {
        purchasePort.find(userId, bookId).ifPresent(p -> {
            if (p.active()) {
                purchasePort.save(p.withActive(false));
                log.info("Revoked library access: user={} book={}", userId, bookId);
            }
        });
    }
}
