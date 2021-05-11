package uk.gov.hmcts.reform.wapostdeploymentfttests.domain.entities.gov;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode
@ToString
public class UkHolidayDates {
    @JsonProperty("england-and-wales")
    private CountryHolidayDates englandAndWales;

    private UkHolidayDates() {
    }

    public UkHolidayDates(CountryHolidayDates englandAndWales) {
        this.englandAndWales = englandAndWales;
    }

    public CountryHolidayDates getEnglandAndWales() {
        return englandAndWales;
    }

}
