package uk.gov.hmcts.reform.sendletter.services;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sendletter.config.ReportsServiceConfig;
import uk.gov.hmcts.reform.sendletter.entity.LettersCountSummaryRepository;
import uk.gov.hmcts.reform.sendletter.entity.reports.ServiceLettersCount;
import uk.gov.hmcts.reform.sendletter.model.out.LettersCountSummary;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sendletter.util.TimeZones.localDateTimeWithUtc;

@ExtendWith(MockitoExtension.class)
class ReportsServiceTest {

    @Mock
    LettersCountSummaryRepository repository;

    @Mock
    private ZeroRowFiller zeroRowFiller;

    @Mock
    private ReportsServiceConfig reportsServiceConfig;

    private ReportsService service;

    @BeforeEach
    void setUp() {
        this.service = new ReportsService(this.repository, reportsServiceConfig, zeroRowFiller, "16:00", "17:00");

        when(this.zeroRowFiller.fill(any()))
            .thenAnswer(invocation -> invocation.getArgument(0)); // return data unchanged
    }

    @Test
    void should_return_letters_count_for_the_date() {
        LocalDate date = LocalDate.of(2019, 4, 25);
        LocalTime timeFrom = LocalTime.parse("17:00");
        LocalTime timeTo = LocalTime.parse("16:00");

        //given
        given(repository.countByDate(
            localDateTimeWithUtc(date.minusDays(1), timeFrom),
            localDateTimeWithUtc(date, timeTo))
        ).willReturn(Stream.of(
            new ServiceLettersCount("aService", 10),
            new ServiceLettersCount("bService", 20)
        ));

        given(reportsServiceConfig.getServiceConfig())
            .willReturn(ImmutableMap.of("aService", "FolderA", "bService", "FolderB"));

        //when
        List<LettersCountSummary> result = service.getCountFor(date);

        //then
        assertThat(result)
            .isNotEmpty()
            .hasSize(2)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new LettersCountSummary("FolderA", 10),
                new LettersCountSummary("FolderB", 20));
    }

    @Test
    void should_return_letters_count_excluding_the_test_service() {
        LocalDate date = LocalDate.of(2019, 4, 25);
        LocalTime timeFrom = LocalTime.parse("17:00");
        LocalTime timeTo = LocalTime.parse("16:00");

        //given
        given(repository.countByDate(
            localDateTimeWithUtc(date.minusDays(1), timeFrom),
            localDateTimeWithUtc(date, timeTo))
        ).willReturn(Stream.of(
            new ServiceLettersCount("aService", 10),
            new ServiceLettersCount("send_letter_tests", 20)
        ));

        given(reportsServiceConfig.getServiceConfig())
            .willReturn(ImmutableMap.of("aService", "FolderA", "send_letter_tests", "Bulk Print"));

        //when
        List<LettersCountSummary> result = service.getCountFor(date);

        //then
        assertThat(result).isNotEmpty()
            .hasSize(1)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(new LettersCountSummary("FolderA", 10));
    }

    @Test
    void should_return_letters_count_excluding_the_nulls() {
        LocalDate date = LocalDate.of(2019, 4, 25);
        LocalTime timeFrom = LocalTime.parse("17:00");
        LocalTime timeTo = LocalTime.parse("16:00");

        //given
        given(repository.countByDate(
            localDateTimeWithUtc(date.minusDays(1), timeFrom),
            localDateTimeWithUtc(date, timeTo))
        ).willReturn(Stream.of(
            new ServiceLettersCount("aService", 10),
            new ServiceLettersCount(null, 2)
        ));
        given(reportsServiceConfig.getServiceConfig()).willReturn(ImmutableMap.of("aService", "FolderA"));

        //when
        List<LettersCountSummary> result = service.getCountFor(date);

        //then
        assertThat(result).isNotEmpty()
            .hasSize(1)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(new LettersCountSummary("FolderA", 10));
    }

    @Test
    void should_map_empty_list_from_repo() {
        LocalDate date = LocalDate.of(2019, 4, 25);
        LocalTime timeFrom = LocalTime.parse("17:00");
        LocalTime timeTo = LocalTime.parse("16:00");

        //given
        given(repository.countByDate(
            localDateTimeWithUtc(date.minusDays(1), timeFrom),
            localDateTimeWithUtc(date, timeTo))
        ).willReturn(Stream.empty());

        //when
        List<LettersCountSummary> result = service.getCountFor(date);

        //then
        assertThat(result).isEmpty();
    }

}
