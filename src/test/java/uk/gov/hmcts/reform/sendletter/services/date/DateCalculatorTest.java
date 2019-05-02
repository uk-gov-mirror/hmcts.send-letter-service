package uk.gov.hmcts.reform.sendletter.services.date;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.ZonedDateTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class DateCalculatorTest {

    private final DateCalculator dateCalculator = new DateCalculator();

    @ParameterizedTest
    @MethodSource("provideTestCaseData")
    public void subtractBusinessDays_should_subtract_the_right_number_of_week_days(
        String baseDate,
        int daysToSubtract,
        String expectResult
    ) {
        ZonedDateTime dateTime = ZonedDateTime.parse(baseDate);
        ZonedDateTime result = dateCalculator.subtractBusinessDays(dateTime, daysToSubtract);
        assertThat(result).isEqualTo(expectResult);
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
