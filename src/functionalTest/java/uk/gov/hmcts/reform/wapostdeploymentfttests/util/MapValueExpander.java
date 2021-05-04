package uk.gov.hmcts.reform.wapostdeploymentfttests.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNullElse;

@Component
@SuppressWarnings("unchecked")
public final class MapValueExpander {

    public static final Properties ENVIRONMENT_PROPERTIES = new Properties(System.getProperties());
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";
    private static final Pattern TODAY_PATTERN = Pattern.compile("\\{\\$TODAY([+-]?\\d*?)}");
    private static final Pattern DATETIME_TODAY_PATTERN = Pattern.compile("\\{\\$DATETIME-TODAY([+-]?\\d*?)}");
    private static final Pattern GENERATED_CASE_ID_PATTERN = Pattern.compile("\\{\\$GENERATED_CASE_ID}");
    private static final Pattern RANDOM_UUID_PATTERN = Pattern.compile("\\{\\$RANDOM_UUID}");
    private static final Pattern USER_ID_PATTERN = Pattern.compile("\\{\\$USER_ID}");
    private static final Pattern VERIFIER_PATTERN = Pattern.compile("\\{\\$VERIFIER-(.*?)}");
    private static final Pattern ENVIRONMENT_PROPERTY_PATTERN = Pattern.compile("\\{\\$([a-zA-Z0-9].+?)}");

    private MapValueExpander() {
        // noop
    }

    private static String expandToday(String value) {

        Matcher matcher = TODAY_PATTERN.matcher(value);

        String expandedValue = value;

        while (matcher.find()) {

            char plusOrMinus = '+';
            int dayAdjustment = 0;

            if (matcher.groupCount() == 1
                && !matcher.group(1).isEmpty()) {

                plusOrMinus = matcher.group(1).charAt(0);
                dayAdjustment = Integer.parseInt(matcher.group(1).substring(1));
            }

            LocalDate now = LocalDate.now();

            if (plusOrMinus == '+') {
                now = now.plusDays(dayAdjustment);
            } else {
                now = now.minusDays(dayAdjustment);
            }

            String token = matcher.group(0);

            expandedValue = expandedValue.replace(token, now.toString());
        }

        return expandedValue;
    }


    public static String expandDateTimeToday(String value) {

        Matcher matcher = DATETIME_TODAY_PATTERN.matcher(value);

        String expandedValue = value;

        while (matcher.find()) {

            char plusOrMinus = '+';
            int dayAdjustment = 0;

            if (matcher.groupCount() == 1
                && !matcher.group(1).isEmpty()) {

                plusOrMinus = matcher.group(1).charAt(0);
                dayAdjustment = Integer.parseInt(matcher.group(1).substring(1));
            }

            LocalDateTime now = LocalDateTime.now();

            if (plusOrMinus == '+') {
                now = now.plusDays(dayAdjustment);
            } else {
                now = now.minusDays(dayAdjustment);
            }

            String token = matcher.group(0);

            expandedValue = expandedValue.replace(token, now.format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)));
        }

        return expandedValue;
    }

    public void expandValues(Map<String, Object> map, Map<String, String> additionalValues) {

        for (Map.Entry<String, Object> entry : map.entrySet()) {

            Object untypedValue = entry.getValue();

            if (untypedValue instanceof List) {

                untypedValue =
                    ((List) untypedValue)
                        .stream()
                        .map(value -> expandValue(value, additionalValues))
                        .collect(Collectors.toList());

            } else {
                untypedValue = expandValue(untypedValue, additionalValues);
            }

            entry.setValue(untypedValue);
        }
    }

    private Object expandValue(Object untypedValue, Map<String, String> additionalValues) {

        if (untypedValue instanceof Map) {

            expandValues((Map<String, Object>) untypedValue, additionalValues);

        } else if (untypedValue instanceof String) {

            String value = (String) untypedValue;

            //If the value is not a verifier then expand
            if (!VERIFIER_PATTERN.matcher(value).find()) {

                if (TODAY_PATTERN.matcher(value).find()) {
                    value = expandToday(value);
                }

                if (DATETIME_TODAY_PATTERN.matcher(value).find()) {
                    value = expandDateTimeToday(value);
                }

                if (RANDOM_UUID_PATTERN.matcher(value).find()) {
                    value = expandRandomUuid(value);
                }

                if (GENERATED_CASE_ID_PATTERN.matcher(value).find()) {
                    if (!additionalValues.isEmpty()) {
                        value = expandCaseId(value, additionalValues.get("caseId"));
                    }
                }

                if (USER_ID_PATTERN.matcher(value).find()) {
                    if (!additionalValues.isEmpty()) {
                        value = expandUserId(value, additionalValues.get("userId"));
                    }
                }

                if (ENVIRONMENT_PROPERTY_PATTERN.matcher(value).find()) {
                    value = expandEnvironmentProperty(value);
                }
            }

            return value;
        }

        return untypedValue;
    }

    private String expandRandomUuid(String value) {
        Matcher matcher = RANDOM_UUID_PATTERN.matcher(value);
        String expandedValue = value;

        while (matcher.find()) {

            String token = matcher.group(0);

            expandedValue = expandedValue.replace(token, UUID.randomUUID().toString());
        }

        return expandedValue;
    }


    private String expandUserId(String value, String replacementValue) {
        Matcher matcher = USER_ID_PATTERN.matcher(value);
        String expandedValue = value;

        if (replacementValue != null) {

            while (matcher.find()) {

                String token = matcher.group(0);

                expandedValue = expandedValue.replace(token, replacementValue);
            }
        }

        return expandedValue;
    }

    private String expandCaseId(String value, String replacementValue) {
        Matcher matcher = GENERATED_CASE_ID_PATTERN.matcher(value);
        String expandedValue = value;

        if (replacementValue != null) {
            while (matcher.find()) {

                String token = matcher.group(0);

                expandedValue = expandedValue.replace(token, replacementValue);
            }
        }
        return expandedValue;
    }

    private String expandEnvironmentProperty(String value) {

        Matcher matcher = ENVIRONMENT_PROPERTY_PATTERN.matcher(value);

        String expandedValue = value;

        while (matcher.find()) {

            if (matcher.groupCount() == 1
                && !matcher.group(1).isEmpty()) {

                String token = matcher.group(0);
                String propertyName = matcher.group(1);

                String property = ENVIRONMENT_PROPERTIES.getProperty(propertyName);

                expandedValue = expandedValue.replace(token, requireNonNullElse(property, ""));
            }
        }


        return expandedValue;
    }
}
