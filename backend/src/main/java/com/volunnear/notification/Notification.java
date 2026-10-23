package com.volunnear.notification;

/**
 * One message to one recipient. It is also the Spring event that {@link Notifier} publishes inside the
 * business transaction and {@link NotificationDispatcher} sends after the commit (Q7f).
 */
public record Notification(String to, String subject, String body) {
}
