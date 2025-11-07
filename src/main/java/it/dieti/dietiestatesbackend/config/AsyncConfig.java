package it.dieti.dietiestatesbackend.config;

import it.dieti.dietiestatesbackend.application.notification.NotificationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor(NotificationProperties props) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(props.getExecutorCorePoolSize());
        executor.setMaxPoolSize(props.getExecutorMaxPoolSize());
        executor.setQueueCapacity(props.getExecutorQueueCapacity());
        executor.setThreadNamePrefix("notif-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        executor.setTaskDecorator(r -> () -> {
            try {
                r.run();
            } catch (RuntimeException ex) {
                log.error("Async task failed in notificationExecutor", ex);
                throw ex;
            }
        });
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new AsyncUncaughtExceptionHandler() {
            @Override
            public void handleUncaughtException(Throwable ex, Method method, Object... params) {
                log.error("Unhandled exception in @Async method {} with params {}", method, params, ex);
            }
        };
    }
}
