package uk.gov.hmcts.reform.wapostdeploymentfttests.domain.entities;

import lombok.*;

@Getter
public class CalculateDateParameters {

    final char plusOrMinus;
    final int dayAdjustment;
    final boolean workingDays;

    public CalculateDateParameters(char plusOrMinus, int dayAdjustment, boolean workingDays) {
        this.plusOrMinus = plusOrMinus;
        this.dayAdjustment = dayAdjustment;
        this.workingDays = workingDays;
    }


}
