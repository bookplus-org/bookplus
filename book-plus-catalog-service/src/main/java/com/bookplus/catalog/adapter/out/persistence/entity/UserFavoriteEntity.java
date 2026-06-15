package com.bookplus.catalog.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/** Favorito de un usuario (clave compuesta user_id + book_id). */
@Entity
@Table(name = "user_favorites")
@IdClass(UserFavoriteEntity.PK.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserFavoriteEntity {

    @Id
    @Column(name = "user_id")
    private String userId;

    @Id
    @Column(name = "book_id", columnDefinition = "uuid")
    private UUID bookId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class PK implements Serializable {
        private String userId;
        private UUID   bookId;
    }
}
