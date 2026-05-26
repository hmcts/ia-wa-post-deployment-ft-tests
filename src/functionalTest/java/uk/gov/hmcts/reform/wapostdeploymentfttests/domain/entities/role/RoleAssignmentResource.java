package uk.gov.hmcts.reform.wapostdeploymentfttests.domain.entities.role;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@Getter
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoleAssignmentResource {
    private List<RoleAssignment> roleAssignmentResponse;

    public RoleAssignmentResource() {
        //Default constructor
    }

    public RoleAssignmentResource(List<RoleAssignment> roleAssignmentResponse) {
        this.roleAssignmentResponse = roleAssignmentResponse;
    }

}
