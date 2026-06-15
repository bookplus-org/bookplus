package com.bookplus.notification.application.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Builds HTML email bodies for each notification type.
 * In production this could use Thymeleaf templates stored in resources/templates/email/.
 * Here we keep it simple with inline HTML strings for portability.
 */
@Service
public class EmailTemplateService {

    private static final String BRAND = "BookPlus";
    private static final String FOOTER = """
            <br/><hr/>
            <p style="color:#888;font-size:12px">
              © %s %s. All rights reserved.<br/>
              You're receiving this email because you have an account with us.
            </p>
            """.formatted(java.time.Year.now(), BRAND);

    public String orderCreated(String orderId, String total, String currency) {
        return wrap("Order Received — Thank you!", """
                <h2>We've received your order!</h2>
                <p>Order ID: <strong>%s</strong></p>
                <p>Total: <strong>%s %s</strong></p>
                <p>We'll send you another email once your payment is confirmed.</p>
                """.formatted(orderId, total, currency));
    }

    public String orderConfirmed(String orderId) {
        return wrap("Order Confirmed!", """
                <h2>Your order is confirmed!</h2>
                <p>Order ID: <strong>%s</strong></p>
                <p>Your payment was successful. We're now preparing your books for shipment.</p>
                """.formatted(orderId));
    }

    public String orderShipped(String orderId) {
        return wrap("Your Order Has Shipped!", """
                <h2>Your books are on their way!</h2>
                <p>Order ID: <strong>%s</strong></p>
                <p>Your order has been dispatched and will arrive soon.</p>
                """.formatted(orderId));
    }

    public String orderDelivered(String orderId) {
        return wrap("Order Delivered", """
                <h2>Your order has been delivered!</h2>
                <p>Order ID: <strong>%s</strong></p>
                <p>Enjoy your books! If you have any issues, please contact our support team.</p>
                """.formatted(orderId));
    }

    public String orderCancelled(String orderId, String reason) {
        return wrap("Order Cancelled", """
                <h2>Your order has been cancelled.</h2>
                <p>Order ID: <strong>%s</strong></p>
                <p>Reason: %s</p>
                <p>If a payment was made, a refund will be processed within 3-5 business days.</p>
                """.formatted(orderId, reason));
    }

    public String orderRefunded(String orderId, String reason) {
        return wrap("Order Refunded", """
                <h2>Your order has been refunded.</h2>
                <p>Order ID: <strong>%s</strong></p>
                <p>Reason: %s</p>
                <p>The amount has been refunded to your original payment method. It may take
                   3-5 business days to appear, depending on your bank.</p>
                """.formatted(orderId, reason));
    }

    public String paymentCompleted(String orderId, String amount, String currency, String txRef) {
        return wrap("Payment Successful", """
                <h2>Payment received!</h2>
                <p>Order ID: <strong>%s</strong></p>
                <p>Amount: <strong>%s %s</strong></p>
                <p>Transaction reference: %s</p>
                """.formatted(orderId, amount, currency, txRef));
    }

    public String paymentFailed(String orderId, String reason) {
        return wrap("Payment Failed", """
                <h2>We could not process your payment.</h2>
                <p>Order ID: <strong>%s</strong></p>
                <p>Reason: %s</p>
                <p>Please try again or use a different payment method.</p>
                """.formatted(orderId, reason));
    }

    public String refundInitiated(String orderId, String amount, String currency) {
        return wrap("Refund Initiated", """
                <h2>Your refund is on its way.</h2>
                <p>Order ID: <strong>%s</strong></p>
                <p>Refund amount: <strong>%s %s</strong></p>
                <p>Please allow 3-5 business days for the amount to appear in your account.</p>
                """.formatted(orderId, amount, currency));
    }

    public String lowStockAlert(String bookTitle, String bookId, int available) {
        return wrap("[ALERT] Low Stock", """
                <h2>Low stock alert</h2>
                <p>Book: <strong>%s</strong> (ID: %s)</p>
                <p>Available units: <strong>%d</strong></p>
                <p>Please restock soon to avoid lost sales.</p>
                """.formatted(bookTitle, bookId, available));
    }

    // ── Private ───────────────────────────────────────────────────────────

    private String wrap(String title, String content) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head><meta charset="UTF-8"><title>%s — %s</title></head>
                <body style="font-family:Arial,sans-serif;max-width:600px;margin:auto;padding:20px">
                  <div style="background:#1a1a2e;padding:16px;border-radius:8px 8px 0 0">
                    <h1 style="color:#e94560;margin:0">%s</h1>
                  </div>
                  <div style="border:1px solid #eee;border-top:none;padding:20px">
                    %s
                    %s
                  </div>
                </body>
                </html>
                """.formatted(BRAND, title, BRAND, content, FOOTER);
    }
}
