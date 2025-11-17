package it.dieti.dietiestatesbackend.application.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmailDispatchService {
    private final NotificationProperties properties;
    private final EmailQueueService queueService;

    public EmailDispatchService(NotificationProperties properties, EmailQueueService queueService) {
        this.properties = properties;
        this.queueService = queueService;
    }

    public void sendEmailSync(String recipient, String subject, String text) {
        // For compatibility with phase 1: enqueue and process inline (blocking) in current thread
        var id = queueService.enqueue(recipient, subject, text);
        queueService.processOne(id, false);
    }

    public void sendEmailAsync(String recipient, String subject, String text) {
        // Enqueue and trigger async processing of this specific email
        var id = queueService.enqueue(recipient, subject, text);
        queueService.processQueuedAsync(id);
    }
}
