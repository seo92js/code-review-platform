package com.seojs.code_review_platform.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AccessLogFilter extends OncePerRequestFilter {

    private static final Logger ACCESS_LOGGER = LoggerFactory.getLogger("ACCESS_LOG");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            String method = request.getMethod();
            String uri = request.getRequestURI();
            int status = response.getStatus();
            String remoteAddr = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            // [IP] [Method] [URI] [Status] [Duration] [User-Agent]
            ACCESS_LOGGER.info("[{}] [{} {}] [{}] [{}ms] [{}]",
                    remoteAddr, method, uri, status, duration, userAgent);
        }
    }
}
