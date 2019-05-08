package uk.gov.hmcts.reform.sendletter.services.date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.web.client.RestClientException;
import uk.gov.hmcts.reform.sendletter.services.date.holidays.BankHolidaysClient;
import uk.gov.hmcts.reform.sendletter.services.date.holidays.response.Event;
import uk.gov.hmcts.reform.sendletter.services.date.holidays.response.Holidays;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class DateCalculatorTest {

    private BankHolidaysClient bankHolidaysClient;

    private DateCalculator dateCalculator;

    @BeforeEach
    void setUp() {
        this.bankHolidaysClient = mock(BankHolidaysClient.class);
        dateCalculator = new DateCalculator(bankHolidaysClient);
    }

    @ParameterizedTest
    @MethodSource("provideTestCaseData")
    public void subtractBusinessDays_should_subtract_the_right_number_of_week_days(
        String baseDate,
        int daysToSubtract,
        String expectResult
    ) {
        given(bankHolidaysClient.getHolidays()).willReturn(new Holidays(emptyList()));

        ZonedDateTime dateTime = ZonedDateTime.parse(baseDate);
        ZonedDateTime result = dateCalculator.subtractBusinessDays(dateTime, daysToSubtract);
        assertThat(result).isEqualTo(expectResult);
    }

    @Test
    void should_take_bank_holidays_into_account() {
        // given
        LocalDate christmasDay = LocalDate.of(2019, 12, 25);
        given(bankHolidaysClient.getHolidays())
            .willReturn(new Holidays(singletonList(
                new Event(christmasDay, "Christmas Day")
            )));

        // sanity check, not Saturday or Sunday
        assertThat(christmasDay.getDayOfWeek()).isEqualTo(DayOfWeek.WEDNESDAY);

        // when
        ZonedDateTime result = this.dateCalculator.subtractBusinessDays(
            ZonedDateTime.parse("2019-12-26T12:00:00Z"),
            1
        );

        // then
        assertThat(result).isEqualTo(ZonedDateTime.parse("2019-12-24T12:00:00Z"));
    }

    @Test
    void should_work_if_fetching_holidays_fails() {
        // given
        given(bankHolidaysClient.getHolidays()).willThrow(RestClientException.class);

        // when
        ZonedDateTime result = this.dateCalculator.subtractBusinessDays(
            ZonedDateTime.parse("2019-12-26T12:00:00Z"), // Thursday
            1
        );

        // then
        assertThat(result).isEqualTo(ZonedDateTime.parse("2019-12-25T12:00:00Z"));
    }

    private static Stream<Arguments> provideTestCaseData() {
        return Stream.of(
            // base date, number of days to subtract, expected result

            // expecting same date when subtracting zero days
            Arguments.of("2019-05-06T12:34:56Z", 0, "2019-05-06T12:34:56Z"),
            Arguments.of("2019-05-05T12:34:56Z", 0, "2019-05-05T12:34:56Z"),
            Arguments.of("2019-05-04T12:34:56Z", 0, "2019-05-04T12:34:56Z"),
            Arguments.of("2019-05-03T12:34:56Z", 0, "2019-05-03T12:34:56Z"),
            Arguments.of("2019-05-02T12:34:56Z", 0, "2019-05-02T12:34:56Z"),
            Arguments.of("2019-05-01T12:34:56Z", 0, "2019-05-01T12:34:56Z"),
            Arguments.of("2019-04-30T12:34:56Z", 0, "2019-04-30T12:34:56Z"),

            // expecting the previous week day when subtracting one day
            Arguments.of("2019-05-06T12:34:56Z", 1, "2019-05-03T12:34:56Z"),
            Arguments.of("2019-05-05T12:34:56Z", 1, "2019-05-03T12:34:56Z"),
            Arguments.of("2019-05-04T12:34:56Z", 1, "2019-05-03T12:34:56Z"),
            Arguments.of("2019-05-03T12:34:56Z", 1, "2019-05-02T12:34:56Z"),
            Arguments.of("2019-05-02T12:34:56Z", 1, "2019-05-01T12:34:56Z"),
            Arguments.of("2019-05-01T12:34:56Z", 1, "2019-04-30T12:34:56Z"),
            Arguments.of("2019-04-30T12:34:56Z", 1, "2019-04-29T12:34:56Z"),

            // subtracting different numbers of days
            Arguments.of("2019-05-06T12:34:56Z", 2, "2019-05-02T12:34:56Z"),
            Arguments.of("2019-05-06T12:34:56Z", 3, "2019-05-01T12:34:56Z"),
            Arguments.of("2019-05-06T12:34:56Z", 4, "2019-04-30T12:34:56Z"),
            Arguments.of("2019-05-06T12:34:56Z", 5, "2019-04-29T12:34:56Z"),
            Arguments.of("2019-05-06T12:34:56Z", 6, "2019-04-26T12:34:56Z"),

            // expecting same time and time zone
            Arguments.of("2019-05-06T23:59:59.999+01:00", 1, "2019-05-03T23:59:59.999+01:00"),
            Arguments.of("2019-05-01T00:00:00+02:00", 1, "2019-04-30T00:00:00+02:00")
        );
    }
}
