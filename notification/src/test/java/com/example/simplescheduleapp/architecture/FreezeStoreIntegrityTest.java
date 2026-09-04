package com.example.simplescheduleapp.architecture;

import com.example.simplescheduleapp.support.FreezeStoreIntegrity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * notification 모듈 freeze 스토어 무결성 검사.
 *
 * <p>규칙이 존재하는 것과 규칙이 강제되는 것은 다르다. 규칙 서술이 바뀌면 ArchUnit은 새 UUID
 * 스토어에 위반을 통째로 다시 동결하고 초록으로 통과한다. 그 순간 남는 고아 엔트리를 잡는 것이
 * 이 테스트다. 자세한 배경은 {@link FreezeStoreIntegrity} 참고.
 */
class FreezeStoreIntegrityTest {

    @Test
    @DisplayName("freeze 스토어에 고아 엔트리와 고아 파일이 없다")
    void freeze_스토어에_고아가_없다() {
        FreezeStoreIntegrity.assertNoOrphans(HexagonalRulesTest.class);
    }
}
