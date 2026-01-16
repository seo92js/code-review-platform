# Code Review Platform (Backend)

## 1. 서비스 소개
**Code Review Platform**은 GitHub Pull Request를 연동하여 AI가 코드 리뷰를 수행하는 서비스입니다.
개발자가 작성한 코드의 변경 사항을 AI가 분석하고, 개선점이나 잠재적인 버그를 찾아 GitHub 코멘트로 피드백을 제공합니다.
GitHub 계정으로 로그인하여 리뷰를 원하는 PR을 선택하고, 버튼 하나로 AI 리뷰를 요청할 수 있습니다.

## 2. 주요 기능
- **AI 코드 리뷰**: 복잡한 설정 없이, 클릭 한 번으로 코드 품질을 분석하고 개선안을 제안받습니다.
- **맞춤형 리뷰어**: 팀의 코딩 컨벤션이나 선호하는 스타일에 맞춰 AI의 리뷰 성향(프롬프트)을 자유롭게 조정할 수 있습니다.
- **유연한 모델 선택**: 원하는 AI 모델을 선택해 리뷰를 진행할 수 있습니다.
- **실시간 동기화**: 웹훅(Webhook) 기술을 통해 GitHub의 PR 생성, 수정, 머지 상태를 실시간으로 감지하고 반영합니다.
- **간편한 연동**: 별도의 회원가입 없이 GitHub 계정으로 즉시 로그인하고, 리포지토리를 원클릭으로 연결합니다.
- **강력한 보안**: 사용자의 리포지토리 접근 권한(Token)은 최신 암호화 기술로 안전하게 보호됩니다.

## 3. 기술 스택
- **Language**: Java 17
- **Framework**: Spring Boot 3.4.2
- **Database**: PostgreSQL (Production), H2 (Local)
- **ORM**: Spring Data JPA, QueryDSL
- **AI Integration**: Spring AI
- **Security**: Spring Security OAuth2, AES Encryption
- **Build**: Gradle

## 4. 시스템 아키텍처 및 배포
이 프로젝트는 **GitHub Actions**와 **Docker**, **GHCR** 를 활용한 CI/CD 파이프라인으로 배포됩니다.