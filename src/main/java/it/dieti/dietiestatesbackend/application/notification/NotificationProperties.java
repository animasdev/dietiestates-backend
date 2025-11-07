package it.dieti.dietiestatesbackend.application.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@ConfigurationProperties(prefix = "app.notification")
public class NotificationProperties {
    /**
     * Toggle to enable/disable email sending without touching code.
     */
    private boolean enabled = false;

    /**
     * Email address used as sender.
     */
    private String fromEmail = "noreply@dietiestates.it";

    /**
     * Optional friendly name for the sender.
     */
    private String fromName = "DietiEstates";

    /**
     * Enable asynchronous dispatch of emails via @Async executor.
     */
    private boolean asyncEnabled = false;

    /**
     * Executor configuration for async email dispatch.
     */
    private int executorCorePoolSize = 2;
    private int executorMaxPoolSize = 4;
    private int executorQueueCapacity = 100;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getFromEmail() {
        return fromEmail;
    }

    public void setFromEmail(String fromEmail) {
        this.fromEmail = fromEmail;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public boolean isAsyncEnabled() {
        return asyncEnabled;
    }

    public void setAsyncEnabled(boolean asyncEnabled) {
        this.asyncEnabled = asyncEnabled;
    }

    public int getExecutorCorePoolSize() {
        return executorCorePoolSize;
    }

    public void setExecutorCorePoolSize(int executorCorePoolSize) {
        this.executorCorePoolSize = executorCorePoolSize;
    }

    public int getExecutorMaxPoolSize() {
        return executorMaxPoolSize;
    }

    public void setExecutorMaxPoolSize(int executorMaxPoolSize) {
        this.executorMaxPoolSize = executorMaxPoolSize;
    }

    public int getExecutorQueueCapacity() {
        return executorQueueCapacity;
    }

    public void setExecutorQueueCapacity(int executorQueueCapacity) {
        this.executorQueueCapacity = executorQueueCapacity;
    }

    public String formattedFromAddress() {
        if (StringUtils.hasText(fromName)) {
            return fromName + " <" + fromEmail + ">";
        }
        return fromEmail;
    }
}
