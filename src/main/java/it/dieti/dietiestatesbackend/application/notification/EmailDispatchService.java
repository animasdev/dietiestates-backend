package it.dieti.dietiestatesbackend.application.notification;

import org.springframework.stereotype.Service;

@Service
public class EmailDispatchService {
    private final EmailQueueService queueService;

    public EmailDispatchService(EmailQueueService queueService) {
        this.queueService = queueService;
    }

    public void sendEmailSync(String recipient, String subject, String text) {
        var id = queueService.enqueue(recipient, subject, text);
        queueService.processOne(id, false);
    }

    public void sendEmailAsync(String recipient, String subject, String text) {
        var id = queueService.enqueue(recipient, subject, text);
        queueService.processQueuedAsync(id);
    }
}
