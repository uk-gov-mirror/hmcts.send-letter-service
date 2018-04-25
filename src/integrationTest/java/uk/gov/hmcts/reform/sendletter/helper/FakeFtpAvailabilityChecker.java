package uk.gov.hmcts.reform.sendletter.helper;

import uk.gov.hmcts.reform.sendletter.services.ftp.IFtpAvailabilityChecker;

import java.time.LocalTime;

public class FakeFtpAvailabilityChecker implements IFtpAvailabilityChecker {

    private boolean isAvailable = false;

    @Override
    public boolean isFtpAvailable(LocalTime time) {
        return this.isAvailable;
    }

    @Override
    public LocalTime getDowntimeStart() {
        return LocalTime.now();
    }

    public void setAvailable(boolean value) {
        isAvailable = value;
    }
}
