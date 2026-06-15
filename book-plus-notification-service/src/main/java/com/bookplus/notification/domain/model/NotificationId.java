package com.bookplus.notification.domain.model;
import java.util.UUID;
public record NotificationId(UUID value) {
    public static NotificationId generate()    { return new NotificationId(UUID.randomUUID()); }
    public static NotificationId of(String id) { return new NotificationId(UUID.fromString(id)); }
    public static NotificationId of(UUID id)   { return new NotificationId(id); }
    @Override public String toString()         { return value.toString(); }
}
