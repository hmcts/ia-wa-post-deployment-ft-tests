package uk.gov.hmcts.reform.wapostdeploymentfttests.util;

import java.util.regex.Pattern;

public final class RegularExpressions {

    public static final String UUID_REGEX_PATTERN =
        "[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}";
    public static final Pattern TODAY_PATTERN = Pattern.compile("\\{\\$TODAY([+-]?\\d*?)?(_WORKING_DAYS)?}");
    public static final Pattern DATETIME_TODAY_PATTERN =
        Pattern.compile("\\{\\$DATETIME_TODAY([+-]?\\d*?)?(_WORKING_DAYS)?}");
    public static final Pattern GENERATED_CASE_ID_PATTERN = Pattern.compile("\\{\\$GENERATED_CASE_ID}");
    public static final Pattern RANDOM_UUID_PATTERN = Pattern.compile("\\{\\$RANDOM_UUID}");
    public static final Pattern USER_ID_PATTERN = Pattern.compile("\\{\\$USER_ID}");
    public static final Pattern ENVIRONMENT_PROPERTY_PATTERN = Pattern.compile("\\{\\$([a-zA-Z0-9].+?)}");
    public static final Pattern VERIFIER_PATTERN = Pattern.compile("\\{\\$VERIFIER-(.*?)}");
    public static final Pattern VERIFIER_DATETIME_TODAY_WORKING_DAYS_PATTERN =
        Pattern.compile("\\{\\$VERIFIER-DATETIME_TODAY([+-]?\\d*?)?(_WORKING_DAYS)?}");


    private RegularExpressions() {
        //noop
    }

}
