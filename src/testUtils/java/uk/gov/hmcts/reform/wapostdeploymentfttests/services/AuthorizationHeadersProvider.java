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
import uk.gov.hmcts.reform.wapostdeploymentfttests.domain.entities.idam.UserInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class AuthorizationHeadersProvider {

    public static final String AUTHORIZATION = "Authorization";
    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    private final Map<String, String> tokens = new ConcurrentHashMap<>();
    private final Map<String, UserInfo> userInfo = new ConcurrentHashMap<>();
    @Value("${idam.redirectUrl}")
    protected String idamRedirectUrl;
    @Value("${idam.scope}")
    protected String userScope;
    @Value("${spring.security.oauth2.client.registration.oidc.client-id}")
    protected String idamClientId;
    @Value("${spring.security.oauth2.client.registration.oidc.client-secret}")
    protected String idamClientSecret;

    @Autowired
    private IdamWebApi idamWebApi;

    @Autowired
    private AuthTokenGenerator serviceAuthTokenGenerator;

    public Header getServiceAuthorizationHeader() {
        String serviceToken = tokens.computeIfAbsent(
            SERVICE_AUTHORIZATION,
            user -> serviceAuthTokenGenerator.generate()
        );

        return new Header(SERVICE_AUTHORIZATION, serviceToken);
    }

    public Headers getTribunalCaseworkerAAuthorization() {
        return new Headers(
            getCaseworkerAAuthorizationOnly(),
            getServiceAuthorizationHeader()
        );
    }


    public Headers getLegalRepAuthorization() {
        /*
         * This user is used to create cases in ccd and inject messages
         */
        return new Headers(
            getLawFirmAuthorizationOnly(),
            getServiceAuthorizationHeader()
        );
    }


    public Header getCaseworkerAAuthorizationOnly() {

        String username = System.getenv("TEST_WA_CASEOFFICER_PUBLIC_A_USERNAME");
        String password = System.getenv("TEST_WA_CASEOFFICER_PUBLIC_A_PASSWORD");

        return getAuthorization("Caseworker A", username, password);
    }

    public Header getLawFirmAuthorizationOnly() {

        String username = System.getenv("TEST_WA_LAW_FIRM_USERNAME");
        String password = System.getenv("TEST_WA_LAW_FIRM_PASSWORD");

        return getAuthorization("LawFirm", username, password);

    }

    private Header getAuthorization(String key, String username, String password) {

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

    public UserInfo getUserInfo(String userToken) {
        return userInfo.computeIfAbsent(
            userToken,
            user -> idamWebApi.userInfo(userToken)
        );

    }

    public Headers getServiceAuthorizationHeadersOnly() {
        return new Headers(getServiceAuthorizationHeader());
    }
}
