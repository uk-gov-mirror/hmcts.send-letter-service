package uk.gov.hmcts.reform.sendletter.services.ftp;

import java.time.LocalTime;

public interface IFtpAvailabilityChecker {

    boolean isFtpAvailable(LocalTime time);

    LocalTime getDowntimeStart();
}
