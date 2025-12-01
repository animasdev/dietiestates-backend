package it.dieti.dietiestatesbackend.application.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private EmailDispatchService dispatchService;

    private NotificationProperties properties;

    @InjectMocks
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        properties = new NotificationProperties();
        properties.setEnabled(true);
        properties.setFromEmail("noreply@dietiestates.it");
        properties.setFromName("DietiEstates");
        // default to sync for first test
        properties.setAsyncEnabled(false);
        notificationService = new NotificationService(dispatchService, properties);
    }

    @Test
    void sendSignUpConfirmation_syncDelegatesWithContent() {
        notificationService.sendSignUpConfirmation("user@example.com", "ABC123");

        ArgumentCaptor<String> recipient = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);

        verify(dispatchService).sendEmailSync(recipient.capture(), subject.capture(), body.capture());

        assertThat(recipient.getValue()).isEqualTo("user@example.com");
        assertThat(subject.getValue()).isEqualTo("Conferma registrazione DietiEstates");
        assertThat(body.getValue()).contains("ABC123");
    }

    @Test
    void sendPasswordReset_asyncDelegatesWithContent() {
        properties.setAsyncEnabled(true);
        notificationService = new NotificationService(dispatchService, properties);

        notificationService.sendPasswordReset("user@example.com", "RESET");

        ArgumentCaptor<String> recipient = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);

        verify(dispatchService).sendEmailAsync(recipient.capture(), subject.capture(), body.capture());

        assertThat(recipient.getValue()).isEqualTo("user@example.com");
        assertThat(subject.getValue()).isEqualTo("Reset password DietiEstates");
        assertThat(body.getValue()).contains("RESET");
    }
}
