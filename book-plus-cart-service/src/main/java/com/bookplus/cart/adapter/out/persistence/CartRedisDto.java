package com.bookplus.cart.adapter.out.persistence;

import com.bookplus.cart.domain.model.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Flat DTO serialized to Redis as JSON.
 * Deliberately avoids domain types so serialization stays stable
 * even if domain model changes (Jackson can still deserialize old snapshots).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record CartRedisDto(
        String            id,
        String            userId,
        List<ItemDto>     items,
        Instant           createdAt,
        Instant           updatedAt
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ItemDto(
            String     bookId,
            String     isbn,
            String     title,
            String     imageUrl,
            BigDecimal unitPrice,
            String     currency,
            int        quantity
    ) {
        static ItemDto from(CartItem item) {
            return new ItemDto(
                    item.getBookId().toString(),
                    item.getIsbn(),
                    item.getTitle(),
                    item.getImageUrl(),
                    item.getUnitPrice().amount(),
                    item.getUnitPrice().currency(),
                    item.getQuantity()
            );
        }

        CartItem toDomain() {
            return CartItem.create(
                    BookId.of(bookId),
                    title,
                    imageUrl,
                    isbn,
                    quantity,
                    Money.of(unitPrice, currency)
            );
        }
    }

    // ── from domain ───────────────────────────────────────────────────────

    static CartRedisDto from(Cart cart) {
        List<ItemDto> items = cart.getItems().stream()
                .map(ItemDto::from)
                .toList();
        return new CartRedisDto(
                cart.getId().toString(),
                cart.getUserId(),
                items,
                cart.getCreatedAt(),
                cart.getUpdatedAt()
        );
    }

    // ── to domain ─────────────────────────────────────────────────────────

    Cart toDomain() {
        Map<BookId, CartItem> itemMap = new LinkedHashMap<>();
        for (ItemDto dto : items) {
            CartItem item = dto.toDomain();
            itemMap.put(item.getBookId(), item);
        }
        return Cart.reconstitute(
                CartId.of(UUID.fromString(id)),
                userId,
                itemMap,
                createdAt,
                updatedAt
        );
    }
}
