package com.bookplus.catalog.domain.model;

import com.bookplus.catalog.domain.event.*;
import com.bookplus.catalog.domain.exception.DomainException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/**
 * Aggregate Root — Libro del catálogo.
 *
 * Encapsula toda la lógica de negocio del catálogo: precios, descuentos,
 * reseñas, rating promedio, stock disponible (desnormalizado).
 *
 * Stock real pertenece a inventory-service. Aquí guardamos un snapshot
 * actualizado por eventos Kafka (StockUpdated) para mostrar disponibilidad.
 */
public class Book {

    private final BookId    id;
    private final ISBN      isbn;
    private String          title;
    private Slug            slug;
    private String          author;
    private String          description;
    private Money           price;
    private Money           discountPrice;   // nullable
    private String          imageUrl;
    private String          previewUrl;      // nullable — muestra/preview (PDF)
    private String          publisher;
    private LocalDate       publishedDate;
    private String          language;
    private Integer         pages;
    private CategoryId      categoryId;
    private boolean         active;
    private int             stockSnapshot;   // desnormalizado desde inventory-service
    private BigDecimal      averageRating;   // desnormalizado desde reseñas
    private int             reviewCount;
    private final Instant   createdAt;
    private Instant         updatedAt;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    // ── Constructor privado ───────────────────────────────────────────────

    private Book(BookId id, ISBN isbn, String title, Slug slug, String author,
                 String description, Money price, Money discountPrice, String imageUrl,
                 String previewUrl, String publisher, LocalDate publishedDate, String language,
                 Integer pages, CategoryId categoryId, boolean active, int stockSnapshot,
                 BigDecimal averageRating, int reviewCount,
                 Instant createdAt, Instant updatedAt) {
        this.id            = Objects.requireNonNull(id);
        this.isbn          = Objects.requireNonNull(isbn);
        this.title         = validateTitle(title);
        this.slug          = Objects.requireNonNull(slug);
        this.author        = validateAuthor(author);
        this.description   = description;
        this.price         = Objects.requireNonNull(price);
        this.discountPrice = discountPrice;
        this.imageUrl      = imageUrl;
        this.previewUrl    = previewUrl;
        this.publisher     = publisher;
        this.publishedDate = publishedDate;
        this.language      = language;
        this.pages         = pages;
        this.categoryId    = Objects.requireNonNull(categoryId);
        this.active        = active;
        this.stockSnapshot = Math.max(0, stockSnapshot);
        this.averageRating = averageRating != null ? averageRating : BigDecimal.ZERO;
        this.reviewCount   = Math.max(0, reviewCount);
        this.createdAt     = Objects.requireNonNull(createdAt);
        this.updatedAt     = Objects.requireNonNull(updatedAt);
    }

    // ── Factory Methods ───────────────────────────────────────────────────

    public static Book create(ISBN isbn, String title, String author, String description,
                              Money price, String imageUrl, String previewUrl, String publisher,
                              LocalDate publishedDate, String language, Integer pages,
                              CategoryId categoryId) {
        if (title == null || title.isBlank()) {
            throw new com.bookplus.catalog.domain.exception.DomainException("Book title must not be blank");
        }
        Instant now = Instant.now();
        Book book = new Book(
                BookId.generate(), isbn, title, Slug.from(title), author, description,
                price, null, imageUrl, previewUrl, publisher, publishedDate, language, pages,
                categoryId, true, 0, BigDecimal.ZERO, 0, now, now
        );
        book.registerEvent(new BookCreatedEvent(book.id, book.isbn, book.title,
                book.author, book.categoryId, book.price));
        return book;
    }

    public static Book reconstitute(BookId id, ISBN isbn, String title, Slug slug,
                                    String author, String description, Money price,
                                    Money discountPrice, String imageUrl, String previewUrl,
                                    String publisher, LocalDate publishedDate, String language,
                                    Integer pages, CategoryId categoryId, boolean active,
                                    int stockSnapshot, BigDecimal averageRating, int reviewCount,
                                    Instant createdAt, Instant updatedAt) {
        return new Book(id, isbn, title, slug, author, description, price, discountPrice,
                imageUrl, previewUrl, publisher, publishedDate, language, pages, categoryId, active,
                stockSnapshot, averageRating, reviewCount, createdAt, updatedAt);
    }

    // ── Comportamientos de Dominio ────────────────────────────────────────

    public void update(String title, String author, String description, Money price,
                       Money discountPrice, String imageUrl, String previewUrl, String publisher,
                       LocalDate publishedDate, String language, Integer pages,
                       CategoryId categoryId) {
        this.title         = validateTitle(title);
        this.slug          = Slug.from(title);
        this.author        = validateAuthor(author);
        this.description   = description;
        this.price         = Objects.requireNonNull(price);
        this.discountPrice = discountPrice;
        this.imageUrl      = imageUrl;
        this.previewUrl    = previewUrl;
        this.publisher     = publisher;
        this.publishedDate = publishedDate;
        this.language      = language;
        this.pages         = pages;
        this.categoryId    = Objects.requireNonNull(categoryId);
        this.updatedAt     = Instant.now();
        registerEvent(new BookUpdatedEvent(this.id, this.isbn, this.title,
                this.author, this.categoryId, this.price));
    }

    public void deactivate() {
        if (!this.active) throw new DomainException("Book is already inactive: " + isbn);
        this.active    = false;
        this.updatedAt = Instant.now();
        registerEvent(new BookDeletedEvent(this.id, this.isbn));
    }

    public void applyDiscount(Money discountPrice) {
        if (discountPrice.isGreaterThan(this.price)) {
            throw new DomainException("Discount price cannot exceed regular price");
        }
        this.discountPrice = discountPrice;
        this.updatedAt     = Instant.now();
    }

    public void removeDiscount() {
        this.discountPrice = null;
        this.updatedAt     = Instant.now();
    }

    /** Llamado por inventory-service via Kafka event StockUpdated */
    public void updateStock(int newStock) {
        this.stockSnapshot = Math.max(0, newStock);
        this.updatedAt     = Instant.now();
    }

    /** Recalcula rating promedio al agregar una nueva reseña */
    public void addReviewStats(Rating newRating) {
        int totalRating = this.averageRating.multiply(
                BigDecimal.valueOf(reviewCount)).intValue() + newRating.value();
        this.reviewCount++;
        this.averageRating = BigDecimal.valueOf(totalRating)
                .divide(BigDecimal.valueOf(reviewCount), 2, RoundingMode.HALF_UP);
        this.updatedAt = Instant.now();
    }

    public boolean isInStock()       { return stockSnapshot > 0; }
    public boolean hasDiscount()     { return discountPrice != null; }

    public Money effectivePrice() {
        return hasDiscount() ? discountPrice : price;
    }

    // ── Domain Events ─────────────────────────────────────────────────────

    private void registerEvent(DomainEvent event) { domainEvents.add(event); }

    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return Collections.unmodifiableList(events);
    }

    // ── Validaciones ──────────────────────────────────────────────────────

    private static String validateTitle(String title) {
        if (title == null || title.isBlank()) throw new DomainException("Book title must not be blank");
        if (title.length() > 255) throw new DomainException("Book title must not exceed 255 chars");
        return title.trim();
    }

    private static String validateAuthor(String author) {
        if (author == null || author.isBlank()) throw new DomainException("Author must not be blank");
        if (author.length() > 255) throw new DomainException("Author must not exceed 255 chars");
        return author.trim();
    }

    // ── Getters ───────────────────────────────────────────────────────────

    public BookId      getId()            { return id; }
    public ISBN        getIsbn()          { return isbn; }
    public String      getTitle()         { return title; }
    public Slug        getSlug()          { return slug; }
    public String      getAuthor()        { return author; }
    public String      getDescription()   { return description; }
    public Money       getPrice()         { return price; }
    public Money       getDiscountPrice() { return discountPrice; }
    public String      getImageUrl()      { return imageUrl; }
    public String      getPreviewUrl()    { return previewUrl; }
    public String      getPublisher()     { return publisher; }
    public LocalDate   getPublishedDate() { return publishedDate; }
    public String      getLanguage()      { return language; }
    public Integer     getPages()         { return pages; }
    public CategoryId  getCategoryId()    { return categoryId; }
    public boolean     isActive()         { return active; }
    public int         getStockSnapshot() { return stockSnapshot; }
    public BigDecimal  getAverageRating() { return averageRating; }
    public int         getReviewCount()   { return reviewCount; }
    public Instant     getCreatedAt()     { return createdAt; }
    public Instant     getUpdatedAt()     { return updatedAt; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Book b)) return false;
        return id.equals(b.id);
    }
    @Override public int hashCode()  { return Objects.hash(id); }
    @Override public String toString() {
        return "Book{id=%s, isbn=%s, title='%s'}".formatted(id, isbn, title);
    }
}
