package com.seojs.code_review_platform.analytics.service;

import com.seojs.code_review_platform.analytics.entity.PageViewLog;
import com.seojs.code_review_platform.analytics.repository.PageViewLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PageViewService {

    private final PageViewLogRepository pageViewLogRepository;

    @Async
    @Transactional
    public void recordPageView(String sessionId, String path, String ipAddress, String userAgent) {
        PageViewLog log = PageViewLog.builder()
                .sessionId(sessionId)
                .path(path)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        pageViewLogRepository.save(log);
    }
}
