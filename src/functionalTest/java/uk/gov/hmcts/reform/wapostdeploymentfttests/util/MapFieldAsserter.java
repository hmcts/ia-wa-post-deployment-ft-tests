package uk.gov.hmcts.reform.wapostdeploymentfttests.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.MapValueExpander.DATE_TIME_FORMAT;

@SuppressWarnings("unchecked")
public final class MapFieldAsserter {
    private static final Pattern VERIFIER_DATETIME_TODAY_PATTERN =
        Pattern.compile("\\{\\$VERIFIER-DATETIME-WORKING-DAYS-TODAY([+-]?\\d*?)}");
    private static final String UUID_REGEX_PATTERN =
        "[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}";
    private static SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd");
    private static SimpleDateFormat DATE_TIME_FORMATTER = new SimpleDateFormat(DATE_TIME_FORMAT);

    private MapFieldAsserter() {
        // noop
    }

    public static void assertFields(
        Map<String, Object> expectedMap,
        Map<String, Object> actualMap,
        final String path
    ) {
        for (Map.Entry<String, Object> expectedEntry : expectedMap.entrySet()) {

            String key = expectedEntry.getKey();
            String pathWithKey = path + "." + key;

            Object expectedValue = expectedEntry.getValue();
            Object actualValue = actualMap.get(key);


            if ((expectedValue instanceof List) && (actualValue instanceof List)) {

                List expectedValueCollection = (List) expectedValue;
                List actualValueCollection = (List) actualValue;

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

                assertValue(expectedValue, actualValue, pathWithKey);
            }
        }
    }

    private static void assertValue(
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

            if ((expectedValue instanceof String) && (actualValue instanceof String)) {

                String expectedValueString = (String) expectedValue;
                String actualValueString = (String) actualValue;


                if (expectedValueString.equals("{$VERIFIER-UUID}")) {

                    assertTrue(
                        "Expected field did not match UUID regular expression (" + path + ")",
                        actualValueString.matches(UUID_REGEX_PATTERN)
                    );
                } else if (VERIFIER_DATETIME_TODAY_PATTERN.matcher(expectedValueString).find()) {

                    //TODO: This needs to be working days so need to add logic to workout the date in working days.

                    expectedValueString = expectedValueString.replace("VERIFIER-", "");
                    String expandedExpectedDate = MapValueExpander.expandDateTimeToday(expectedValueString);

                    Date expectedDate = null;
                    try {
                        expectedDate = DATE_TIME_FORMATTER.parse(expandedExpectedDate);

                    } catch (ParseException e) {
                        fail("Could not parse expected date in (" + path + ")");
                    }

                    Date actualDate = null;

                    try {
                        actualDate = DATE_TIME_FORMATTER.parse(actualValueString);

                    } catch (ParseException e) {
                        fail("Could not parse actual date in (" + path + ")");
                    }

                    assertEquals(
                        "Expected field did not match actual (" + path + ")",
                        DATE_FORMATTER.format(expectedDate),
                        DATE_FORMATTER.format(actualDate)

                    );
                } else if (expectedValueString.length() > 3
                           && expectedValueString.startsWith("$/")
                           && expectedValueString.endsWith("/")) {

                    expectedValueString = expectedValueString.substring(2, expectedValueString.length() - 1);

                    assertThat(
                        "Expected field matches regular expression (" + path + ")",
                        actualValueString,
                        matchesPattern(expectedValueString)
                    );

                    return;
                }
            } else {
                assertThat(
                    "Expected field matches (" + path + ")",
                    actualValue,
                    equalTo(expectedValue)
                );
            }
        }
    }
}
