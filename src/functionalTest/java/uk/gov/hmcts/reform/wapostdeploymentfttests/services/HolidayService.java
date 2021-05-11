package uk.gov.hmcts.reform.wapostdeploymentfttests.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wapostdeploymentfttests.clients.GovUkHolidayDatesClient;
import uk.gov.hmcts.reform.wapostdeploymentfttests.domain.entities.gov.HolidayDate;
import uk.gov.hmcts.reform.wapostdeploymentfttests.domain.entities.gov.UkHolidayDates;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class HolidayService {
    private final List<LocalDate> holidays;
    private final GovUkHolidayDatesClient govUkHolidayDatesClient;

    @Autowired
    public HolidayService(List<LocalDate> holidays, GovUkHolidayDatesClient govUkHolidayDatesClient) {
        this.holidays = holidays;
        this.govUkHolidayDatesClient = govUkHolidayDatesClient;
    }

    @Bean
    public List<LocalDate> loadHolidays() {
        UkHolidayDates holidayDates = govUkHolidayDatesClient.getHolidayDates();
        return holidayDates.getEnglandAndWales().getEvents().stream()
            .map(HolidayDate::getDate)
            .collect(Collectors.toList());
    }

    public boolean isHoliday(LocalDate localDate) {
        return holidays.contains(localDate);
    }
}
