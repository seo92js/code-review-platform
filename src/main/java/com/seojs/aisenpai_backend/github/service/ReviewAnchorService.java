package com.seojs.aisenpai_backend.github.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ReviewAnchorService {

    /**
     * Patch 내에서 codeSnippet이 위치한 라인의 GitHub 코멘트용 라인 번호 조회
     */
    public Integer findLineNumber(String patch, String codeSnippet) {
        if (patch == null || codeSnippet == null || patch.isEmpty() || codeSnippet.isEmpty()) {
            return null;
        }

        // 코드가 너무 길면 첫 줄만 사용
        String targetLine = codeSnippet.lines().findFirst().orElse("").trim();
        if (targetLine.isEmpty()) {
            return null;
        }

        // Patch 파싱 및 검색
        String[] patchLines = patch.split("\\R");
        int currentLineInFile = 0;

        for (String line : patchLines) {
            String trimmedLine = line.trim();

            if (trimmedLine.startsWith("@@")) {
                try {
                    String[] parts = trimmedLine.split("\\s+");
                    if (parts.length >= 3) {
                        String newInfo = parts[2];
                        String cleanNewInfo = newInfo.startsWith("+") ? newInfo.substring(1) : newInfo;
                        String startLineStr = cleanNewInfo.split(",")[0];

                        currentLineInFile = Integer.parseInt(startLineStr) - 1; // 0-based context
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse hunk header: '{}'. Error: {}", line, e.getMessage());
                    currentLineInFile = -1;
                }
                continue;
            }

            if (currentLineInFile == -1) {
                continue;
            }

            if (line.startsWith(" ")) {
                currentLineInFile++;
            } else if (line.startsWith("+")) {
                currentLineInFile++;
                // 추가된 라인(변경분)이므로 검색 대상
                String cleanLine = line.substring(1).trim();

                // 검색어 포함 여부 확인
                if (cleanLine.contains(targetLine)) {
                    return currentLineInFile;
                }
            }
        }

        return null;
    }
}
