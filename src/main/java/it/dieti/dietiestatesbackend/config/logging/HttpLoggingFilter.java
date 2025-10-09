package it.dieti.dietiestatesbackend.config.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Component
public class HttpLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(HttpLoggingFilter.class);
    private static final int MAX_PAYLOAD_LENGTH = 4096;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (shouldSkip(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        var cachingRequest = wrapRequest(request);
        var cachingResponse = wrapResponse(response);

        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(cachingRequest, cachingResponse);
        } finally {
            long duration = System.currentTimeMillis() - start;
            logRequestAndResponse(cachingRequest, cachingResponse, duration);
            cachingResponse.copyBodyToResponse();
        }
    }

    private boolean shouldSkip(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/actuator") || uri.startsWith("/swagger") || uri.startsWith("/v3/api-docs") || uri.startsWith("/media/");
    }

    private ContentCachingRequestWrapper wrapRequest(HttpServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper wrapper) {
            return wrapper;
        }
        return new ContentCachingRequestWrapper(request, MAX_PAYLOAD_LENGTH);
    }

    private ContentCachingResponseWrapper wrapResponse(HttpServletResponse response) {
        if (response instanceof ContentCachingResponseWrapper wrapper) {
            return wrapper;
        }
        return new ContentCachingResponseWrapper(response);
    }

    private void logRequestAndResponse(ContentCachingRequestWrapper request,
                                       ContentCachingResponseWrapper response,
                                       long durationMillis) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        int status = response.getStatus();

        String requestBody = readLimitedPayload(request.getContentAsByteArray(), request.getCharacterEncoding());
        String responseBody = readLimitedPayload(response.getContentAsByteArray(), response.getCharacterEncoding());

        log.info("HTTP {} {}{} | status={} | duration={}ms | requestBody={} | responseBody={}",
                method,
                uri,
                query != null ? "?" + query : "",
                status,
                durationMillis,
                requestBody,
                responseBody);
    }

    private String readLimitedPayload(byte[] content, String characterEncoding) {
        if (content == null || content.length == 0) {
            return "<empty>";
        }
        Charset charset = StandardCharsets.UTF_8;
        if (characterEncoding != null) {
            try {
                charset = Charset.forName(characterEncoding);
            } catch (UnsupportedCharsetException ex) {
                log.warn("Unsupported charset '{}' while logging payload, falling back to UTF-8", characterEncoding);
            }
        }
        String body = new String(content, charset);
        if (body.length() > MAX_PAYLOAD_LENGTH) {
            return body.substring(0, MAX_PAYLOAD_LENGTH) + "...";
        }
        return body;
    }
}
