package uk.gov.hmcts.reform.wapostdeploymentfttests.domain.entities.idam;

import lombok.Getter;

@Getter
public class RoleCode {
    private final String code;

    public RoleCode(String code) {
        this.code = code;
    }

}
