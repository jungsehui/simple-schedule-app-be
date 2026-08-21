package com.example.simplescheduleapp.support;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.lang.ArchRule;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FreezingArchRule 위반 스토어({@code src/test/archunit-violations/})의 무결성 검사.
 *
 * <h2>막으려는 사고</h2>
 * <p>세 모듈 모두 {@code freeze.store.default.allowStoreCreation=true}다. 이 설정에서는 규칙의
 * 서술 문자열({@code .because(...)} 포함)이 한 글자만 바뀌어도 ArchUnit이 이를 <b>새 규칙</b>으로
 * 보고 새 UUID 스토어를 만든다. 그러면서 <b>기존 위반 전부를 새 기준선으로 다시 동결</b>하고
 * 테스트는 초록으로 통과한다. 옛 엔트리는 지워지지 않고 고아로 남는다.
 * (근거: ArchUnit {@code TextFileBasedViolationStore.ensureRuleFileName} 의 else 분기와
 * {@code FreezingArchRule.evaluate} 의 {@code !store.contains(delegate)} 분기.)
 *
 * <p>즉 래칫이 조용히 리셋되고 아무도 모른다. 파일 수만 세는 검사로는 이걸 못 잡는다.
 * 리셋이 일어나면 파일도 엔트리도 <b>함께</b> 하나씩 늘어나 개수가 계속 일치하기 때문이다.
 * 그래서 개수가 아니라 <b>신원</b>을 대조한다.
 *
 * <h2>검사 두 가지</h2>
 * <ol>
 *   <li>{@code stored.rules}의 모든 키는 코드에 실제로 선언된 규칙의 서술과 일치해야 한다.
 *       일치하지 않는 키 = 고아 엔트리 = 규칙 텍스트가 바뀌었다는 증거.</li>
 *   <li>{@code stored.rules}가 가리키는 UUID 파일 집합과 디렉터리의 실제 파일 집합이 같아야 한다.
 *       (브리프가 요구한 "개수 일치"를 신원 일치로 강화한 것이다. 개수 일치는 여기서 자동으로 따라온다.)</li>
 * </ol>
 *
 * <p>반대 방향(선언된 규칙이 전부 스토어에 있어야 한다)은 <b>일부러 검사하지 않는다.</b>
 * 새 규칙을 막 추가한 시점에는 스토어가 아직 없는 것이 정상이고, ArchUnit 엔진과 Jupiter 엔진의
 * 실행 순서가 보장되지 않아 헛된 빨간불이 난다. 위 두 검사는 실행 순서와 무관하게 결정적이다.
 *
 * <h2>사용법</h2>
 * <pre>{@code
 * class FreezeStoreIntegrityTest {
 *     @Test
 *     void 스토어에_고아가_없다() {
 *         FreezeStoreIntegrity.assertNoOrphans(HexagonalRulesTest.class);
 *     }
 * }
 * }</pre>
 *
 * <p>freeze 규칙을 선언한 클래스를 <b>빠짐없이</b> 넘겨야 한다. 빠뜨리면 그 클래스의 엔트리가
 * 고아로 보여 빨간불이 난다(안전한 방향의 실패다).
 */
public final class FreezeStoreIntegrity {

    private static final String STORED_RULES_FILE_NAME = "stored.rules";
    private static final String STORE_PATH_PROPERTY = "freeze.store.default.path";
    private static final String STORE_PATH_DEFAULT = "archunit_store";

    private FreezeStoreIntegrity() {
    }

    /**
     * 호출 모듈의 freeze 스토어에 고아 엔트리와 고아 파일이 없는지 검증한다.
     *
     * @param ruleHolders freeze 규칙을 {@code static ArchRule} 필드로 선언한 클래스들
     */
    public static void assertNoOrphans(Class<?>... ruleHolders) {
        Path storeDir = storeDirectory();
        assertThat(storeDir)
                .as("freeze 스토어 디렉터리(%s)가 있어야 한다. archunit.properties의 %s 설정을 확인하라",
                        storeDir.toAbsolutePath(), STORE_PATH_PROPERTY)
                .isDirectory();

        Properties storedRules = loadStoredRules(storeDir.resolve(STORED_RULES_FILE_NAME));

        Set<String> storedDescriptions = new TreeSet<>();
        Set<String> referencedFiles = new TreeSet<>();
        for (String key : storedRules.stringPropertyNames()) {
            storedDescriptions.add(normalizeLineBreaks(key));
            referencedFiles.add(storedRules.getProperty(key));
        }

        // 빈 스토어면 아래 두 단언이 공집합끼리 비교라 무효 통과한다. 초록불의 뜻이
        // "검사했고 깨끗하다"가 아니라 "검사할 것이 없다"가 되는 것인데, 그게 바로 이 검사가
        // 막으려는 부류의 사고다. declaredDescriptions 가드와 대칭으로 막는다.
        assertThat(storedDescriptions)
                .as("stored.rules에 엔트리가 하나도 없다. 스토어가 비면 아래 대조가 공집합끼리 비교라 "
                        + "무효 통과한다. 스토어가 지워졌거나 커밋에서 누락됐는지 확인하라")
                .isNotEmpty();

        Set<String> declaredDescriptions = declaredRuleDescriptions(ruleHolders);
        assertThat(declaredDescriptions)
                .as("규칙 선언 클래스를 하나도 못 읽었다. ruleHolders 인자가 잘못됐을 가능성이 높다")
                .isNotEmpty();

        // 검사 1. 고아 엔트리 = 규칙 텍스트가 바뀌어 기준선이 리셋됐다는 증거
        assertThat(storedDescriptions)
                .as("stored.rules에 코드의 어떤 규칙과도 대응하지 않는 고아 엔트리가 있다. "
                        + "규칙 서술(.because 포함)이 바뀌면 ArchUnit이 새 UUID 스토어에 위반을 다시 동결하고 "
                        + "옛 기준선은 고아로 남는다. 규칙 텍스트를 되돌리거나, 의도한 변경이면 옛 엔트리와 "
                        + "그 UUID 파일을 함께 지우고 새 기준선을 리뷰하라")
                .isSubsetOf(declaredDescriptions);

        // 검사 2. 엔트리가 가리키는 파일 집합과 실제 파일 집합의 신원 일치
        assertThat(actualStoreFiles(storeDir))
                .as("archunit-violations/의 실제 파일과 stored.rules가 가리키는 파일이 어긋난다. "
                        + "커밋에서 스토어 파일이 누락됐거나 고아 파일이 섞여 들어갔다")
                .containsExactlyInAnyOrderElementsOf(referencedFiles);
    }

    private static Path storeDirectory() {
        // Gradle test 태스크의 workingDir 기본값이 모듈 디렉터리라 상대 경로가 모듈별로 알아서 풀린다.
        return Path.of(ArchConfiguration.get().getPropertyOrDefault(STORE_PATH_PROPERTY, STORE_PATH_DEFAULT));
    }

    private static Properties loadStoredRules(Path storedRulesFile) {
        assertThat(storedRulesFile)
                .as("stored.rules(%s)가 있어야 한다", storedRulesFile.toAbsolutePath())
                .isRegularFile();
        Properties properties = new Properties();
        // ArchUnit이 Properties.store(OutputStream, ...)로 쓰므로 읽기도 InputStream이어야 한다
        // (ISO-8859-1 + \\uXXXX 이스케이프). 줄 수 세기로는 서술에 개행이 있을 때 틀린다.
        try (InputStream in = Files.newInputStream(storedRulesFile)) {
            properties.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException("stored.rules를 읽지 못했다: " + storedRulesFile.toAbsolutePath(), e);
        }
        return properties;
    }

    private static Set<String> actualStoreFiles(Path storeDir) {
        try (Stream<Path> files = Files.list(storeDir)) {
            return files.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> !STORED_RULES_FILE_NAME.equals(name))
                    // .DS_Store 같은 OS 부산물이 빨간불을 내면 검사 자체가 미움받아 지워진다
                    .filter(name -> !name.startsWith("."))
                    .collect(Collectors.toCollection(TreeSet::new));
        } catch (IOException e) {
            throw new UncheckedIOException("스토어 디렉터리를 읽지 못했다: " + storeDir.toAbsolutePath(), e);
        }
    }

    private static Set<String> declaredRuleDescriptions(Class<?>... ruleHolders) {
        Set<String> descriptions = new TreeSet<>();
        for (Class<?> holder : ruleHolders) {
            // @ArchTest 필드는 package-private이라 getFields()로는 안 잡힌다
            for (Field field : holder.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) || !ArchRule.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                descriptions.add(normalizeLineBreaks(readRule(field).getDescription()));
            }
        }
        return descriptions;
    }

    private static ArchRule readRule(Field field) {
        try {
            field.setAccessible(true);
            return (ArchRule) field.get(null);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("규칙 필드를 읽지 못했다: " + field, e);
        }
    }

    /** ArchUnit이 스토어에 쓸 때 적용하는 {@code ensureUnixLineBreaks}와 같은 정규화. */
    private static String normalizeLineBreaks(String value) {
        return value.replace("\r\n", "\n").replace("\r", "\n");
    }
}
