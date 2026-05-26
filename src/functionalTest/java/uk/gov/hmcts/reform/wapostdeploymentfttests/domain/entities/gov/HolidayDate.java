package uk.gov.hmcts.reform.wapostdeploymentfttests.domain.entities.gov;

import lombok.*;

import java.time.LocalDate;

@Getter
@EqualsAndHashCode
@ToString
public class HolidayDate {
    private LocalDate date;

    private HolidayDate() {
    }

    public HolidayDate(LocalDate date) {
        this.date = date;
    }

}
