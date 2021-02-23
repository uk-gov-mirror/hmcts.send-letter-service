package uk.gov.hmcts.reform.sendletter.services.ftp;

import java.time.LocalTime;

public class FtpAvailabilityChecker implements IFtpAvailabilityChecker {

    private final LocalTime downtimeStart;
    private final LocalTime downtimeEnd;
    private final boolean disableDowntime;

    public FtpAvailabilityChecker(String downtimeFromHour, String downtimeToHour, Boolean disableDowntime) {
        this.downtimeStart = LocalTime.parse(downtimeFromHour);
        this.downtimeEnd = LocalTime.parse(downtimeToHour);
        this.disableDowntime=disableDowntime;

    }
    public boolean isFtpAvailable(LocalTime time) {
        if (disableDowntime){
            return true;
        }
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
