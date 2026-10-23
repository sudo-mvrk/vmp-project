package com.volunnear.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Q7f: sends a notification only after the business transaction has committed, so a rolled-back accept
 * sends nothing. The transaction is already committed here, so a sender failure is logged and dropped:
 * it must not reach the caller, whose change has succeeded. No retry, no outbox.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDispatcher {
    private final NotificationSender sender;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommitted(Notification notification) {
        try {
            sender.send(notification);
        } catch (RuntimeException e) {
            log.error("Sending notification \"{}\" to {} failed", notification.subject(), notification.to(), e);
        }
    }
}
