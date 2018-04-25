package uk.gov.hmcts.reform.sendletter.services.ftp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Component
public class FtpAvailabilityChecker implements IFtpAvailabilityChecker {

    private final LocalTime downtimeStart;
    private final LocalTime downtimeEnd;

    public FtpAvailabilityChecker(
        @Value("${ftp.downtime.from}") String downtimeFromHour,
        @Value("${ftp.downtime.to}") String downtimeToHour
    ) {
        this.downtimeStart = LocalTime.parse(downtimeFromHour);
        this.downtimeEnd = LocalTime.parse(downtimeToHour);
    }

    public boolean isFtpAvailable(LocalTime time) {
        if (downtimeStart.isBefore(downtimeEnd)) {
            return time.isBefore(downtimeStart) || time.isAfter(downtimeEnd);
        } else {
            return time.isBefore(downtimeStart) && time.isAfter(downtimeEnd);
        }
    }

    public LocalTime getDowntimeStart() {
        return downtimeStart;
    }
}
