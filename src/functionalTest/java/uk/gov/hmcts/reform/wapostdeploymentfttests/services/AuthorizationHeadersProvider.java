package uk.gov.hmcts.reform.wapostdeploymentfttests.services;

import io.restassured.http.Header;
import io.restassured.http.Headers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wapostdeploymentfttests.clients.IdamWebApi;
import uk.gov.hmcts.reform.wapostdeploymentfttests.domain.entities.idam.CredentialRequest;
import uk.gov.hmcts.reform.wapostdeploymentfttests.domain.entities.idam.UserInfo;

import java.io.IOException;
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

    private final IdamWebApi idamWebApi;

    private final AuthTokenGenerator serviceAuthTokenGenerator;

    @Autowired
    public AuthorizationHeadersProvider(IdamWebApi idamWebApi,
                                        AuthTokenGenerator serviceAuthTokenGenerator) {
        this.idamWebApi = idamWebApi;
        this.serviceAuthTokenGenerator = serviceAuthTokenGenerator;
    }

    @Override
    public Headers getIaUserAuthorization(CredentialRequest request) throws IOException {
        switch (request.getCredentialsKey()) {
            case "WaSystemUser":
                return getWaSystemUserAuthorization();
            case "IALegalRepresentative":
                return getLegalRepAuthorization();
            case "IACaseworker":
                return getTribunalCaseworkerAAuthorization();
            case "CTSCAdmin":
                return getCtscAdminAuthorization();
            case "AdminOfficer":
                return getAdminOfficerAuthorization();
            case "NBCAdmin":
                return getNbcAdminAuthorization();
            default:
                throw new IllegalStateException("Credentials implementation for '"
                                                    + request.getCredentialsKey() + "' not found");
        }
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

    private Header getUserAuthorizationHeader(String key, String username, String password) {

        MultiValueMap<String, String> body = createIdamRequest(
            System.getenv(username),
            System.getenv(password)
        );

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


    public Headers getTribunalCaseworkerAAuthorization() {
        return new Headers(
            getCaseworkerAAuthorizationOnly(),
            getServiceAuthorizationHeader()
        );
    }

    public Headers getCtscAdminAuthorization() {
        return new Headers(
            getCtscAdminAuthorizationOnly(),
            getServiceAuthorizationHeader()
        );
    }

    public Headers getAdminOfficerAuthorization() {
        return new Headers(
            getAdminOfficerAuthorizationOnly(),
            getServiceAuthorizationHeader()
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


    public Header getCaseworkerAAuthorizationOnly() {

        return getUserAuthorizationHeader(
            "Caseworker A",
            "TEST_WA_CASEOFFICER_PUBLIC_A_USERNAME",
            "TEST_WA_CASEOFFICER_PUBLIC_A_PASSWORD"
        );
    }

    public Header getCtscAdminAuthorizationOnly() {

        return getUserAuthorizationHeader(
            "CTSCAdmin",
            "TEST_CTSC_ADMIN_USERNAME",
            "TEST_CTSC_ADMIN_PASSWORD"
        );
    }

    public Header getAdminOfficerAuthorizationOnly() {

        return getUserAuthorizationHeader(
            "AdminOfficer",
            "TEST_ADMINOFFICER_USERNAME",
            "TEST_ADMINOFFICER_PASSWORD"
        );
    }

    public Headers getNbcAdminAuthorization() {
        return new Headers(
            getNbcAdminOfficerAuthorizationOnly(),
            getServiceAuthorizationHeader()
        );
    }

    public Header getNbcAdminOfficerAuthorizationOnly() {

        return getUserAuthorizationHeader(
            "NBCAdmin",
            "TEST_NBC_ADMIN_USERNAME",
            "TEST_NBC_ADMIN_PASSWORD"
        );
    }

}
