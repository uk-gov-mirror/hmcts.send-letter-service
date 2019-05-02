package uk.gov.hmcts.reform.sendletter.services.date;

import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.ZonedDateTime;

@Service
public class DateCalculator {

    public ZonedDateTime subtractBusinessDays(ZonedDateTime dateTime, int numberOfBusinessDays) {
        int daysSubtracted = 0;
        ZonedDateTime adjustedDateTime = dateTime;

        while (daysSubtracted < numberOfBusinessDays) {
            adjustedDateTime = adjustedDateTime.minusDays(1);
            if (isBusinessDay(adjustedDateTime)) {
                daysSubtracted++;
            }
        }

        return adjustedDateTime;
    }

    private boolean isBusinessDay(ZonedDateTime dateTime) {
        return dateTime.getDayOfWeek().getValue() < DayOfWeek.SATURDAY.getValue();
    }
}
