package uk.gov.hmcts.reform.wapostdeploymentfttests.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.RegularExpressions.UUID_REGEX_PATTERN;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.RegularExpressions.VERIFIER_ZONED_DATETIME_TODAY_WORKING_DAYS_PATTERN;

@Slf4j
@Component
@SuppressWarnings("unchecked")
public class MapFieldAsserter {

    private final MapValueExpander mapValueExpander;

    private MapFieldAsserter(MapValueExpander mapValueExpander) {
        this.mapValueExpander = mapValueExpander;
    }

    public void assertFields(
        Map<String, Object> expectedMap,
        Map<String, Object> actualMap,
        final String path
    ) {
        for (Map.Entry<String, Object> expectedEntry : expectedMap.entrySet()) {

            String key = expectedEntry.getKey();
            String pathWithKey = path + "." + key;

            Object expectedValue = expectedEntry.getValue();
            Object actualValue = actualMap.get(key);

            if ((expectedValue instanceof List expectedValueCollection)
                && (actualValue instanceof List actualValueCollection)) {

                if (!actualValueCollection.isEmpty()) {
                    //Get first Item to check the instance
                    Object actualValueCollectionItem = actualValueCollection.getFirst();

                    if ((actualValueCollectionItem instanceof Map)) {

                        for (int i = 0; i < expectedValueCollection.size(); i++) {
                            String pathWithKeyAndIndex = pathWithKey + "." + i;


                            Object expectedValueItem = expectedValueCollection.get(i);
                            Object actualValueItem =
                                i < actualValueCollection.size()
                                    ? actualValueCollection.get(i)
                                    : null;

                            assertValue(expectedValueItem, actualValueItem, pathWithKeyAndIndex);

                        }
                    } else {
                        //The collection was a list of objects assert them using any order
                        assertTrue(
                            new HashSet<>(actualValueCollection).containsAll(expectedValueCollection),
                            "Expected collection did not contain all actual values (" + pathWithKey + ")"
                        );
                        assertEquals(
                            ((List<Object>) expectedValueCollection).size(),
                            ((List<Object>) actualValueCollection).size()
                        );
                    }
                } else {
                    //The collection was empty
                    assertEquals(expectedValue, actualValue);
                }

            } else {

                assertValue(expectedValue, actualValue, pathWithKey);
            }
        }
    }

    private void assertValue(
        Object expectedValue,
        Object actualValue,
        String path
    ) {
        if ((expectedValue instanceof Map) && (actualValue instanceof Map)) {

            assertFields(
                (Map<String, Object>) expectedValue,
                (Map<String, Object>) actualValue,
                path
            );

        } else {

            if ((expectedValue instanceof String expectedValueString)
                && (actualValue instanceof String actualValueString)) {

                if (expectedValueString.equals("{$VERIFIER-UUID}")) {

                    assertTrue(
                        actualValueString.matches(UUID_REGEX_PATTERN),
                        "Expected field did not match UUID regular expression (" + path + ")"
                    );
                } else if (VERIFIER_ZONED_DATETIME_TODAY_WORKING_DAYS_PATTERN.matcher(expectedValueString).find()) {
                    log.info(
                        "{}: Found verifier for ZonedDateTime today working days in expected value: {}", path,
                        expectedValueString
                    );
                    expectedValueString = expectedValueString.replace("VERIFIER-", "");
                    log.info("{}: Expected value after removing VERIFIER- prefix: {}", path, expectedValueString);
                    String expandedExpectedDate = mapValueExpander.expandDateTimeToday(expectedValueString);
                    log.info("{}: Expanded expected date: {}", path, expandedExpectedDate);

                    LocalDate expectedDate = null;
                    try {
                        expectedDate = LocalDate.parse(expandedExpectedDate.trim());

                    } catch (DateTimeParseException | NumberFormatException e) {
                        fail("Could not parse expected date: " + expandedExpectedDate + " in (" + path + ")");
                    }

                    LocalDate actualDate = null;
                    log.info("{}: Actual value string to parse: {}", path, actualValueString);
                    try {
                        actualDate = OffsetDateTime.parse(
                            actualValueString,
                            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ")
                        ).toLocalDate();

                    } catch (DateTimeParseException | NumberFormatException e) {
                        fail("Could not parse actual date: " + expandedExpectedDate + " in (" + path + ")");
                    }

                    assertEquals(
                        expectedDate,
                        actualDate,
                        "Expected field did not match actual (" + path + ")"
                    );
                } else if (expectedValueString.length() > 3
                    && expectedValueString.startsWith("$/")
                    && expectedValueString.endsWith("/")) {

                    expectedValueString = expectedValueString.substring(2, expectedValueString.length() - 1);
                    assertTrue(
                        actualValueString.matches(expectedValueString),
                        "Expected field did not match regular expression (" + path + ")"
                    );
                } else {
                    assertEquals(
                        expectedValue,
                        actualValue,
                        "Expected field did not match actual (" + path + ")"
                    );
                }
            } else {
                assertEquals(
                    expectedValue,
                    actualValue,
                    "Expected field matches (" + path + ")"
                );
            }
        }
    }
}
