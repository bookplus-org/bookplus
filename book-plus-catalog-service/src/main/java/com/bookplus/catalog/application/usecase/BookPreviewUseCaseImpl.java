package com.bookplus.catalog.application.usecase;

import com.bookplus.catalog.domain.exception.BookNotFoundException;
import com.bookplus.catalog.domain.exception.InvalidPdfException;
import com.bookplus.catalog.domain.model.BookId;
import com.bookplus.catalog.domain.model.BookPreview;
import com.bookplus.catalog.domain.port.in.BookPreviewUseCase;
import com.bookplus.catalog.domain.port.out.BookPreviewPort;
import com.bookplus.catalog.domain.port.out.LoadBookPort;
import com.bookplus.catalog.shared.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.Optional;

/**
 * Genera y sirve la muestra en PDF de un libro: solo las primeras {@value #MAX_PREVIEW_PAGES}
 * páginas. El recorte con PDFBox es lógica de aplicación; la persistencia va tras un puerto.
 */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class BookPreviewUseCaseImpl implements BookPreviewUseCase {

    private static final int MAX_PREVIEW_PAGES = 12;

    private final BookPreviewPort bookPreviewPort;
    private final LoadBookPort     loadBookPort;

    @Override
    public int storePreview(String bookId, byte[] pdfBytes) {
        if (loadBookPort.findById(BookId.of(bookId)).isEmpty()) {
            throw new BookNotFoundException(bookId);
        }
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new InvalidPdfException("El archivo PDF está vacío");
        }

        try (PDDocument source = Loader.loadPDF(pdfBytes)) {
            int sourcePages = source.getNumberOfPages();
            if (sourcePages == 0) {
                throw new InvalidPdfException("El PDF no tiene páginas");
            }
            int take = Math.min(MAX_PREVIEW_PAGES, sourcePages);

            byte[] sample;
            try (PDDocument out = new PDDocument();
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                for (int i = 0; i < take; i++) {
                    out.importPage(source.getPage(i));
                }
                out.save(baos);
                sample = baos.toByteArray();
            }

            bookPreviewPort.save(new BookPreview(
                    bookId, sample, take, sourcePages, pdfBytes, sourcePages, Instant.now()));

            log.info("Preview generado para libro {}: {}/{} páginas", bookId, take, sourcePages);
            return take;
        } catch (InvalidPdfException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("No se pudo procesar el PDF para libro {}: {}", bookId, ex.getMessage());
            throw new InvalidPdfException("Archivo PDF inválido o dañado");
        }
    }

    @Override
    public Optional<BookPreview> getPreview(String bookId) {
        return bookPreviewPort.findByBookId(bookId);
    }
}
