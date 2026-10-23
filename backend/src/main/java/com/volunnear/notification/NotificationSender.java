package com.volunnear.notification;

/** Delivers a notification. Only the logging sender exists (Q7e); an SMTP sender can implement this later. */
public interface NotificationSender {
    void send(Notification notification);
}
