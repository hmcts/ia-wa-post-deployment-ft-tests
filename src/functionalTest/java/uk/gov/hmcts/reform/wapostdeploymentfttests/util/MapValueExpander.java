package uk.gov.hmcts.reform.wapostdeploymentfttests.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wapostdeploymentfttests.domain.entities.CalculateDateParameters;
import uk.gov.hmcts.reform.wapostdeploymentfttests.services.DateProviderService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNullElse;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.RegularExpressions.ENVIRONMENT_PROPERTY_PATTERN;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.RegularExpressions.GENERATED_CASE_ID_PATTERN;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.RegularExpressions.LOCAL_DATETIME_TODAY_PATTERN;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.RegularExpressions.RANDOM_UUID_PATTERN;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.RegularExpressions.TODAY_PATTERN;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.RegularExpressions.USER_ID_PATTERN;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.RegularExpressions.VERIFIER_PATTERN;
import static uk.gov.hmcts.reform.wapostdeploymentfttests.util.RegularExpressions.ZONED_DATETIME_TODAY_PATTERN;

@Component
@SuppressWarnings("unchecked")
public class MapValueExpander {

    public static final Properties ENVIRONMENT_PROPERTIES = new Properties(System.getProperties());
    public static final String LOCAL_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";
    public static final String ZONED_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

    private final DateProviderService dateProviderService;

    @Autowired
    private MapValueExpander(DateProviderService dateProviderService) {
        this.dateProviderService = dateProviderService;
    }

    public String expandLocalDateTimeToday(String value) {

        Matcher matcher = LOCAL_DATETIME_TODAY_PATTERN.matcher(value);

        String expandedValue = value;

        while (matcher.find()) {

            CalculateDateParameters calculateDateParameters = buildDateParameters(matcher);

            LocalDate date = dateProviderService.calculateDate(calculateDateParameters);

            System.out.println("calculated: " + date);
            LocalDateTime dateTime = date.atStartOfDay();
            System.out.println("startOfDay: " + dateTime);
            String token = matcher.group(0);

            expandedValue = expandedValue.replace(
                token,
                dateTime.format(DateTimeFormatter.ofPattern(LOCAL_DATE_TIME_FORMAT))
            );
            System.out.println("expanded: " + expandedValue);
        }

        return expandedValue;
    }

    public String expandDateTimeToday(String value) {

        Matcher matcher = ZONED_DATETIME_TODAY_PATTERN.matcher(value);

        String expandedValue = value;

        while (matcher.find()) {

            CalculateDateParameters calculateDateParameters = buildDateParameters(matcher);

            LocalDate date = dateProviderService.calculateDate(calculateDateParameters);

            ZonedDateTime dateTime = ZonedDateTime.of(date.atStartOfDay(), ZoneId.of("Europe/London"));

            String token = matcher.group(0);

            expandedValue = expandedValue.replace(
                token,
                dateTime.format(DateTimeFormatter.ofPattern(ZONED_DATE_TIME_FORMAT))
            );
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

    private String expandToday(String value) {

        Matcher matcher = TODAY_PATTERN.matcher(value);

        String expandedValue = value;

        while (matcher.find()) {

            CalculateDateParameters calculateDateParameters = buildDateParameters(matcher);

            LocalDate date = dateProviderService.calculateDate(calculateDateParameters);

            String token = matcher.group(0);

            expandedValue = expandedValue.replace(token, date.toString());
        }

        return expandedValue;
    }

    private CalculateDateParameters buildDateParameters(Matcher matcher) {
        char plusOrMinus = '+';
        int dayAdjustment = 0;
        boolean shouldUseWorkingDays = false;

        if (matcher.groupCount() > 1 && !matcher.group(1).isEmpty()) {
            plusOrMinus = matcher.group(1).charAt(0);
            dayAdjustment = Integer.parseInt(matcher.group(1).substring(1));

            if (matcher.groupCount() == 2 && matcher.group(2) != null && !matcher.group(2).isEmpty()) {
                shouldUseWorkingDays = true;
            }
        }

        return new CalculateDateParameters(plusOrMinus, dayAdjustment, shouldUseWorkingDays);
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
                if (LOCAL_DATETIME_TODAY_PATTERN.matcher(value).find()) {
                    value = expandLocalDateTimeToday(value);
                }
                if (ZONED_DATETIME_TODAY_PATTERN.matcher(value).find()) {
                    value = expandDateTimeToday(value);
                }
                if (RANDOM_UUID_PATTERN.matcher(value).find()) {
                    value = expandRandomUuid(value);
                }
                if (GENERATED_CASE_ID_PATTERN.matcher(value).find() && !additionalValues.isEmpty()) {
                    value = expandCaseId(value, additionalValues.get("caseId"));
                }
                if (USER_ID_PATTERN.matcher(value).find() && !additionalValues.isEmpty()) {
                    value = expandUserId(value, additionalValues.get("userId"));
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
