package it.dieti.dietiestatesbackend.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * Minimal ApiUtil to satisfy OpenAPI-generated default methods when generateSupportingFiles is disabled.
 * It is only used to set example responses during default 501 stubs; implementation can be a no-op.
 */
public final class ApiUtil {
    private static final Logger log = LoggerFactory.getLogger(ApiUtil.class);

    private ApiUtil() {}

    public static void setExampleResponse(NativeWebRequest req, String contentType, String example) {
        log.debug("Setting example response {} {} {}", req, contentType, example);
        // no-op in our setup; Swagger UI examples are not critical for runtime
    }
}
