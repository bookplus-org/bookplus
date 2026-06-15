package com.bookplus.order.adapter.in.web.dto;

import com.bookplus.order.domain.model.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        String              orderId,
        String              userId,
        String              cartId,
        String              status,
        List<OrderItemResponse> items,
        BigDecimal          total,
        String              currency,
        ShippingAddressResponse shippingAddress,
        String              paymentMethod,
        String              deliveryType,
        String              paymentId,
        String              carrier,
        String              trackingNumber,
        String              deliveryCode,   // solo se expone al dueño del pedido
        String              receivedBy,
        String              assignedCourier,
        String              assignedCourierName,
        String              claimStatus,
        String              claimReason,
        String              claimResolution,
        String              couponCode,
        BigDecimal          discountAmount,
        Instant             createdAt,
        Instant             updatedAt
) {
    public record ShippingAddressResponse(
            String recipientName,
            String street,
            String city,
            String state,
            String postalCode,
            String country
    ) {
        public static ShippingAddressResponse from(ShippingAddress a) {
            return new ShippingAddressResponse(
                    a.recipientName(), a.street(), a.city(),
                    a.state(), a.postalCode(), a.country()
            );
        }
    }

    /** Para listados/operaciones admin: oculta el código de entrega. */
    public static OrderResponse from(Order order) {
        return from(order, false);
    }

    /** includeDeliveryCode=true solo cuando el solicitante es el dueño del pedido. */
    public static OrderResponse from(Order order, boolean includeDeliveryCode) {
        return new OrderResponse(
                order.getId().toString(),
                order.getUserId(),
                order.getCartId(),
                order.getStatus().name(),
                order.getItems().stream().map(OrderItemResponse::from).toList(),
                order.getTotal().amount(),
                order.getTotal().currency(),
                ShippingAddressResponse.from(order.getShippingAddress()),
                order.getPaymentMethod(),
                order.getDeliveryType(),
                order.getPaymentId(),
                order.getCarrier(),
                order.getTrackingNumber(),
                includeDeliveryCode ? order.getDeliveryCode() : null,
                order.getReceivedBy(),
                order.getAssignedCourier(),
                order.getAssignedCourierName(),
                order.getClaimStatus(),
                order.getClaimReason(),
                order.getClaimResolution(),
                order.getCouponCode(),
                order.getDiscountAmount(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
