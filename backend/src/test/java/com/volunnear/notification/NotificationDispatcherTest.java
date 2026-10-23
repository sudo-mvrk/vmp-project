package com.volunnear.notification;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class NotificationDispatcherTest {
    private static final Notification NOTIFICATION = new Notification("anna@example.com", "Subject", "Body");

    @Test
    void passesTheNotificationToTheSender() {
        List<Notification> sent = new ArrayList<>();

        new NotificationDispatcher(sent::add).onCommitted(NOTIFICATION);

        assertThat(sent).containsExactly(NOTIFICATION);
    }

    @Test
    void aFailingSenderDoesNotReachTheCaller() {
        // Q7f: the business transaction has already committed, so the failure is only logged.
        NotificationDispatcher dispatcher = new NotificationDispatcher(notification -> {
            throw new IllegalStateException("SMTP down");
        });

        assertThatCode(() -> dispatcher.onCommitted(NOTIFICATION)).doesNotThrowAnyException();
    }
}
