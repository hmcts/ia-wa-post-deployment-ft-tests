package uk.gov.hmcts.reform.wapostdeploymentfttests.domain.entities.gov;

import lombok.*;

import java.util.List;

@Getter
@EqualsAndHashCode
@ToString
public class CountryHolidayDates {
    private List<HolidayDate> events;

    private CountryHolidayDates() {
    }

    public CountryHolidayDates(List<HolidayDate> events) {
        this.events = events;
    }

}
