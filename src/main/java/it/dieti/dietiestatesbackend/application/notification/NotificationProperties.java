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

    /**
     * Max attempts for email delivery before giving up.
     */
    private int maxAttempts = 5;

    /**
     * Base backoff in millis for retries.
     */
    private long backoffBaseMillis = 1000L;

    /**
     * Backoff multiplier for each subsequent retry.
     */
    private double backoffMultiplier = 2.0;

    /**
     * Optional support email for escalation on permanent failures.
     */
    private String supportEmail;

    /**
     * Enable scheduled retry polling of queued/failed emails.
     */
    private boolean schedulerEnabled = false;

    /**
     * Fixed delay for scheduler polling in millis.
     */
    private long schedulerFixedDelayMillis = 15000L;

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

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public long getBackoffBaseMillis() {
        return backoffBaseMillis;
    }

    public void setBackoffBaseMillis(long backoffBaseMillis) {
        this.backoffBaseMillis = backoffBaseMillis;
    }

    public double getBackoffMultiplier() {
        return backoffMultiplier;
    }

    public void setBackoffMultiplier(double backoffMultiplier) {
        this.backoffMultiplier = backoffMultiplier;
    }

    public String getSupportEmail() {
        return supportEmail;
    }

    public void setSupportEmail(String supportEmail) {
        this.supportEmail = supportEmail;
    }

    public boolean isSchedulerEnabled() {
        return schedulerEnabled;
    }

    public void setSchedulerEnabled(boolean schedulerEnabled) {
        this.schedulerEnabled = schedulerEnabled;
    }

    public long getSchedulerFixedDelayMillis() {
        return schedulerFixedDelayMillis;
    }

    public void setSchedulerFixedDelayMillis(long schedulerFixedDelayMillis) {
        this.schedulerFixedDelayMillis = schedulerFixedDelayMillis;
    }
}
