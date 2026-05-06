---
description: gradle test continuous 모드 (파일 변경 시 자동 재실행)
allowed-tools: Bash
---

테스트를 watch 모드로 실행. 파일 변경 시 자동 재실행.

## 명령
```bash
./gradlew test --continuous
```

또는 특정 클래스만:
```bash
./gradlew test --continuous --tests "*AuthServiceTest*"
```

## 종료
- `Ctrl+C`로 watch 종료
- 백그라운드에서 돌리려면 `run_in_background: true`로 호출

## 팁
- TDD 사이클에 적합 (red → green → refactor)
- 테스트 실패 시 빠른 피드백
- 빌드 캐시 활용으로 두 번째 실행부터 빠름
