package com.volunnear.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Q7e: one structured log line per notification instead of an email. */
@Slf4j
@Component
public class LoggingNotificationSender implements NotificationSender {
    @Override
    public void send(Notification notification) {
        log.info("Notification to={} subject=\"{}\" body=\"{}\"", notification.to(), notification.subject(), notification.body());
    }
}
