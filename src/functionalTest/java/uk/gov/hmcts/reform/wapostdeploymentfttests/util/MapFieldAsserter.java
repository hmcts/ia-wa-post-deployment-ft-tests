package uk.gov.hmcts.reform.wapostdeploymentfttests.util;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapValueExpander.ZONED_DATE_TIME_FORMAT;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.RegularExpressions.UUID_REGEX_PATTERN;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.RegularExpressions.VERIFIER_ZONED_DATETIME_TODAY_WORKING_DAYS_PATTERN;

@Slf4j
@Component
@SuppressWarnings("unchecked")
public class MapFieldAsserter {

    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat DATE_TIME_FORMATTER = new SimpleDateFormat(ZONED_DATE_TIME_FORMAT);

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
                        for (Object expectedValueItem : expectedValueCollection) {
                            assertCollectionContainsValue(expectedValueItem, actualValueCollection, pathWithKey);
                        }
                    } else {
                        //The collection was a list of objects assert them using any order
                        assertTrue(
                            new HashSet<>(actualValueCollection).containsAll(expectedValueCollection),
                            "Expected collection did not contain all actual values (" + pathWithKey + ")"
                        );
                        assertEquals(((List<Object>) expectedValueCollection).size(),
                                     ((List<Object>) actualValueCollection).size());
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

    private void assertCollectionContainsValue(
        Object expectedValueItem,
        Collection<?> actualCollection,
        String path
    ) {
        for (Object actualValueItem : actualCollection) {
            try {
                assertValue(expectedValueItem, actualValueItem, path);
                return;
            } catch (AssertionError e) {
                // continue
            }
        }
        try {
            fail("Expected value was not found in actual collection (" + path + ")\nExpected value: "
                     + new JSONObject(expectedValueItem.toString()) + "\nActual collection: "
                     + new JSONObject(actualCollection.toString()));
        } catch (JSONException e) {
            fail("Expected value was not found in actual collection (" + path + ")\nExpected value: "
                     + expectedValueItem + "\nActual collection: "
                     + actualCollection);
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

                    expectedValueString = expectedValueString.replace("VERIFIER-", "");
                    String expandedExpectedDate = mapValueExpander.expandDateTimeToday(expectedValueString);

                    Date expectedDate = null;
                    try {
                        expectedDate = DATE_FORMATTER.parse(expandedExpectedDate);

                    } catch (ParseException | NumberFormatException e) {
                        fail("Could not parse expected date in (" + path + ")");
                    }

                    Date actualDate = null;

                    try {
                        actualDate = DATE_FORMATTER.parse(actualValueString);

                    } catch (ParseException | NumberFormatException e) {
                        fail("Could not parse actual date in (" + path + ")");
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
