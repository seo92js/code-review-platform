package com.seojs.aisenpai_backend.github.service;

import com.seojs.aisenpai_backend.github.entity.LoginHistory;
import com.seojs.aisenpai_backend.github.repository.LoginHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginHistoryService {

    private final LoginHistoryRepository loginHistoryRepository;

    @Transactional
    public void recordLogin(String loginId, String ipAddress, String userAgent) {
        LoginHistory history = LoginHistory.builder()
                .loginId(loginId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        loginHistoryRepository.save(history);
    }
}
