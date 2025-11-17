package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.notification;

import it.dieti.dietiestatesbackend.application.notification.EmailStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface EmailNotificationJpaRepository extends JpaRepository<EmailNotificationEntity, UUID> {
    @Query("select e from EmailNotificationEntity e where e.status in (?1) order by e.createdAt asc")
    List<EmailNotificationEntity> findPending(List<EmailStatus> statuses);
}

