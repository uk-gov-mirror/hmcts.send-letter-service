package uk.gov.hmcts.reform.sendletter.services;

import org.assertj.core.api.AbstractBooleanAssert;
import org.junit.Test;
import uk.gov.hmcts.reform.sendletter.services.ftp.FtpAvailabilityChecker;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;

import static org.assertj.core.api.Assertions.assertThat;

public class FtpAvailabilityCheckerTest {

    @Test
    public void ftp_is_available_before_downtime() {
        assertThatFtpCheck("16:00", "17:00", "15:00").isTrue();
    }

    @Test
    public void ftp_is_available_after_downtime() {
        assertThatFtpCheck("08:00", "09:00", "10:00").isTrue();
    }

    @Test
    public void ftp_is_unavailable_during_downtime() {
        assertThatFtpCheck("06:00", "08:00", "07:00").isFalse();
    }

    @Test
    public void ftp_is_unavailable_exactly_at_downtime_start() {
        assertThatFtpCheck("17:30", "20:00", "17:30").isFalse();
    }

    @Test
    public void ftp_is_unavailable_exactly_at_downtime_end() {
        assertThatFtpCheck("17:30", "20:00", "20:00").isFalse();
    }

    @Test
    public void ftp_is_available_outside_of_overnight_downtime() {
        assertThatFtpCheck("23:30", "02:00", "08:14").isTrue();
    }

    @Test
    public void ftp_is_unavailable_during_overnight_downtime_before_midnight() {
        assertThatFtpCheck("23:00", "01:00", "23:59").isFalse();
    }

    @Test
    public void ftp_is_unavailable_during_overnight_downtime_after_midnight() {
        assertThatFtpCheck("23:00", "01:00", "00:01").isFalse();
    }

    @Test
    public void ftp_is_unavailable_for_a_minute() {
        assertThatFtpCheck("01:00", "01:00", "01:00:29").isFalse();
    }

    @Test(expected = DateTimeParseException.class)
    public void throws_exception_if_from_hour_is_invalid() {
        new FtpAvailabilityChecker("1:00", "02:00");
    }

    @Test(expected = DateTimeParseException.class)
    public void throws_exception_if_to_hour_is_invalid() {
        new FtpAvailabilityChecker("01:00", "2:00");
    }

    private static AbstractBooleanAssert<?> assertThatFtpCheck(String fromHour, String toHour, String now) {
        FtpAvailabilityChecker checker = new FtpAvailabilityChecker(fromHour, toHour);
        return assertThat(checker.isFtpAvailable(LocalTime.parse(now)));
    }
}
