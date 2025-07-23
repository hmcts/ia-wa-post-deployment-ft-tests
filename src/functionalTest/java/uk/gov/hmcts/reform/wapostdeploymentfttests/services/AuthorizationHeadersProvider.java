package uk.gov.hmcts.reform.wapostdeploymentfttests.services;

import io.restassured.http.Header;
import io.restassured.http.Headers;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wapostdeploymentfttests.clients.IdamWebApi;
import uk.gov.hmcts.reform.wapostdeploymentfttests.domain.entities.idam.CredentialRequest;
import uk.gov.hmcts.reform.wapostdeploymentfttests.domain.entities.idam.RoleCode;
import uk.gov.hmcts.reform.wapostdeploymentfttests.domain.entities.idam.UserInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@Service
public class AuthorizationHeadersProvider implements AuthorizationHeaders {

    public static final String AUTHORIZATION = "Authorization";
    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    private final Map<String, String> tokens = new ConcurrentHashMap<>();
    private final Map<String, UserInfo> userInfo = new ConcurrentHashMap<>();
    private final Map<String, String> testUserAccounts = new ConcurrentHashMap<>();
    @Value("${idam.redirectUrl}")
    protected String idamRedirectUrl;
    @Value("${idam.scope}")
    protected String userScope;
    @Value("${spring.security.oauth2.client.registration.oidc.client-id}")
    protected String idamClientId;
    @Value("${spring.security.oauth2.client.registration.oidc.client-secret}")
    protected String idamClientSecret;
    @Value("${idam.test.userCleanupEnabled:false}")
    private boolean testUserDeletionEnabled;
    @Value("${idam.system.password:default}")
    protected String systemPassword;

    private final IdamWebApi idamWebApi;

    private final AuthTokenGenerator serviceAuthTokenGenerator;

    private final RoleAssignmentService roleAssignmentService;

    @Autowired
    public AuthorizationHeadersProvider(IdamWebApi idamWebApi,
                                        AuthTokenGenerator serviceAuthTokenGenerator,
                                        RoleAssignmentService roleAssignmentService) {
        this.idamWebApi = idamWebApi;
        this.serviceAuthTokenGenerator = serviceAuthTokenGenerator;
        this.roleAssignmentService = roleAssignmentService;
    }

    @Override
    public void cleanupTestUsers() {
        testUserAccounts.forEach((key, value) -> {
            Headers headers = new Headers(
                getUserAuthorizationHeader(key, value),
                getServiceAuthorizationHeader()
            );
            UserInfo userInfo = getUserInfo(headers.getValue(AUTHORIZATION));
            roleAssignmentService.clearAllRoleAssignments(headers, userInfo);
            deleteAccount(value);
        });
    }

    private void deleteAccount(String username) {
        if (testUserDeletionEnabled) {
            log.info("Deleting test account '{}'", username);
            idamWebApi.deleteTestUser(username);
        } else {
            log.info("Test User deletion feature flag was not enabled, user '{}' was not deleted", username);
        }
    }

    @Override
    public Headers getIaUserAuthorization(CredentialRequest request) {
        return switch (request.getCredentialsKey()) {
            case "WaSystemUser" -> getWaSystemUserAuthorization();
            case "IALegalRepresentative" -> getLegalRepAuthorization();
            case "AdminOfficer", "NBCAdmin", "CTSCAdmin", "IACaseworker" -> getGeneratedUserAuthorization(request);
            default -> throw new IllegalStateException("Credentials implementation for '"
                                                           + request.getCredentialsKey() + "' not found");
        };
    }

    @Override
    public UserInfo getUserInfo(String userToken) {
        return userInfo.computeIfAbsent(
            userToken,
            user -> idamWebApi.userInfo(userToken)
        );
    }

    public Header getServiceAuthorizationHeader() {
        String serviceToken = tokens.computeIfAbsent(
            SERVICE_AUTHORIZATION,
            user -> serviceAuthTokenGenerator.generate()
        );

        return new Header(SERVICE_AUTHORIZATION, serviceToken);
    }

    private Header getUserAuthorizationHeader(String key, String username) {
        return getUserAuthorizationHeader(key, username, systemPassword);
    }

    private Header getUserAuthorizationHeader(String key, String username, String password) {

        MultiValueMap<String, String> body = createIdamRequest(username, password);

        String accessToken = tokens.computeIfAbsent(
            key,
            user -> "Bearer " + idamWebApi.token(body).getAccessToken()
        );
        return new Header(AUTHORIZATION, accessToken);
    }


    private MultiValueMap<String, String> createIdamRequest(String username, String password) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("redirect_uri", idamRedirectUrl);
        body.add("client_id", idamClientId);
        body.add("client_secret", idamClientSecret);
        body.add("username", username);
        body.add("password", password);
        body.add("scope", userScope);

        return body;
    }

    public Headers getLegalRepAuthorization() {
        return new Headers(
            getLawFirmAuthorizationOnly(),
            getServiceAuthorizationHeader()
        );
    }

    public Header getLawFirmAuthorizationOnly() {
        return getUserAuthorizationHeader(
            "LawFirm",
            "TEST_WA_LAW_FIRM_USERNAME",
            "TEST_WA_LAW_FIRM_PASSWORD"
        );
    }

    public Headers getWaSystemUserAuthorization() {
        return new Headers(
            getUserAuthorizationHeader(
                "WaSystemUser",
                "WA_SYSTEM_USERNAME",
                "WA_SYSTEM_PASSWORD"
            ),
            getServiceAuthorizationHeader()
        );
    }

    private String findOrGenerateUserAccount(String credentialsKey) {
        return testUserAccounts.computeIfAbsent(
            credentialsKey,
            user -> generateUserAccount(credentialsKey)
        );
    }

    private String generateUserAccount(String credentialsKey) {
        String emailPrefix = "wa-granular-permission-pdt";
        String userEmail = emailPrefix + UUID.randomUUID() + "@fake.hmcts.net";

        List<RoleCode> requiredRoles = new ArrayList<>(List.of(
            new RoleCode("caseworker"),
            new RoleCode("caseworker-ia")
        ));

        log.info("Attempting to create a new test account {}", userEmail);

        Map<String, Object> body = requestBody(userEmail, requiredRoles);

        idamWebApi.createTestUser(body);

        log.info("Test account created successfully");

        List<String> roleAssignments = findRoleAssignmentRequirements(credentialsKey);
        log.info("Assigning role assignments {}", roleAssignments);

        for (String r : roleAssignments) {
            assignRoleAssignment(userEmail, credentialsKey, r);
        }

        return userEmail;
    }

    private List<String> findRoleAssignmentRequirements(String credentialsKey) {
        List<String> roleAssignments = new ArrayList<>();
        roleAssignments.add("task-supervisor");
        roleAssignments.add("task-allocator");
        roleAssignments.add("hearing-manager");
        roleAssignments.add("hearing-viewer");
        switch (credentialsKey) {
            case "NBCAdmin":
                roleAssignments.add("national-business-centre");
                break;
            case "AdminOfficer":
                roleAssignments.add("hearing-center-admin");
                roleAssignments.add("hmcts-admin");
                break;
            case "CTSCAdmin":
                roleAssignments.add("ctsc");
                roleAssignments.add("hmcts-ctsc");
                break;
            case "IACaseworker":
                roleAssignments.add("hmcts-legal-operations");
                roleAssignments.add("tribunal-caseworker");
                roleAssignments.add("senior-tribunal-caseworker");
                break;
            default:
                throw new IllegalStateException("Credentials implementation for '" + credentialsKey + "' not found");
        }
        return roleAssignments;
    }

    private void assignRoleAssignment(String userEmail, String credentialsKey, String roleName) {
        Headers authenticationHeaders = new Headers(
            getUserAuthorizationHeader(credentialsKey, userEmail),
            getServiceAuthorizationHeader()
        );
        UserInfo userInfo = getUserInfo(authenticationHeaders.getValue(AUTHORIZATION));
        roleAssignmentService.setupRoleAssignment(authenticationHeaders, userInfo, roleName);
    }

    @NotNull
    private Map<String, Object> requestBody(String userEmail, List<RoleCode> requiredRoles) {
        RoleCode userGroup = new RoleCode("caseworker");

        Map<String, Object> body = new ConcurrentHashMap<>();
        body.put("email", userEmail);
        body.put("password", systemPassword);
        body.put("forename", "WAPDTAccount");
        body.put("surname", "Functional");
        body.put("roles", requiredRoles);
        body.put("userGroup", userGroup);
        return body;
    }

    public Headers getGeneratedUserAuthorization(CredentialRequest request) {
        String userEmail = findOrGenerateUserAccount(request.getCredentialsKey());
        return new Headers(
            getUserAuthorizationHeader(request.getCredentialsKey(), userEmail),
            getServiceAuthorizationHeader()
        );
    }
}
