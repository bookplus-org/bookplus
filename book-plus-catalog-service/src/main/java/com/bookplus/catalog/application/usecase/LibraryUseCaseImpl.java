package com.bookplus.catalog.application.usecase;

import com.bookplus.catalog.domain.exception.PurchaseAccessDeniedException;
import com.bookplus.catalog.domain.model.Book;
import com.bookplus.catalog.domain.model.BookId;
import com.bookplus.catalog.domain.model.Purchase;
import com.bookplus.catalog.domain.port.in.LibraryUseCase;
import com.bookplus.catalog.domain.port.out.BookPreviewPort;
import com.bookplus.catalog.domain.port.out.LoadBookPort;
import com.bookplus.catalog.domain.port.out.PurchasePort;
import com.bookplus.catalog.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@UseCase
@RequiredArgsConstructor
public class LibraryUseCaseImpl implements LibraryUseCase {

    private final PurchasePort    purchasePort;
    private final BookPreviewPort bookPreviewPort;
    private final LoadBookPort    loadBookPort;

    @Override
    public List<Book> listPurchasedBooks(String userId) {
        return purchasePort.findActiveBookIds(userId).stream()
                .map(id -> loadBookPort.findById(BookId.of(id)).orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public Optional<PdfContent> downloadFullPdf(String userId, String bookId, boolean isAdmin) {
        boolean owner = purchasePort.hasActiveAccess(userId, bookId);
        if (!isAdmin && !owner) {
            throw new PurchaseAccessDeniedException("No tienes acceso a este libro");
        }
        // Marcar como descargado (relevante para la política de reembolsos de digitales).
        if (owner) {
            purchasePort.find(userId, bookId).ifPresent(p -> {
                if (!p.downloaded()) {
                    purchasePort.save(p.withDownloaded(true));
                }
            });
        }
        return bookPreviewPort.findByBookId(bookId)
                .filter(pv -> pv.fullPdf() != null)
                .map(pv -> new PdfContent(pv.fullPdf(), pv.fullPages()));
    }

    @Override
    public int updateProgress(String userId, String bookId, int percent) {
        int clamped = Math.max(0, Math.min(100, percent));
        Purchase purchase = purchasePort.find(userId, bookId)
                .filter(Purchase::active)
                .orElseThrow(() -> new PurchaseAccessDeniedException("No tienes acceso a este libro"));

        // El progreso solo avanza (evita que un reset baje el umbral de "consumido").
        if (clamped > purchase.readProgress()) {
            purchasePort.save(purchase.withReadProgress(clamped));
            return clamped;
        }
        return purchase.readProgress();
    }

    @Override
    public ConsumptionFacts getConsumption(String userId, String bookId) {
        return purchasePort.find(userId, bookId)
                .map(p -> new ConsumptionFacts(p.downloaded(), p.readProgress(), p.active()))
                .orElseGet(() -> new ConsumptionFacts(false, 0, false));
    }
}
