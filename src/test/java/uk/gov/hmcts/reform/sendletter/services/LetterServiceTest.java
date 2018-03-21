package uk.gov.hmcts.reform.sendletter.services;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.dao.CleanupFailureDataAccessException;
import uk.gov.hmcts.reform.sendletter.SampleData;
import uk.gov.hmcts.reform.sendletter.data.LetterRepository;
import uk.gov.hmcts.reform.sendletter.data.model.DbLetter;
import uk.gov.hmcts.reform.sendletter.exception.LetterNotFoundException;
import uk.gov.hmcts.reform.sendletter.model.in.LetterPrintedAtPatch;
import uk.gov.hmcts.reform.sendletter.model.in.LetterSentToPrintAtPatch;
import uk.gov.hmcts.reform.sendletter.model.out.LetterStatus;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class LetterServiceTest {

    private LetterService service;

    @Mock
    private LetterRepository letterRepository;

    @Before
    public void setUp() {
        service = new LetterService(letterRepository);
    }

    @Test
    public void should_return_letter_status_when_it_is_found_in_database() {
        ZonedDateTime now = LocalDateTime.now().atZone(ZoneId.systemDefault());
        LetterStatus status = new LetterStatus(UUID.randomUUID(), "some-message-id", now, now, now, false);

        given(letterRepository.getLetterStatus(status.id, "service-name")).willReturn(Optional.of(status));

        assertThat(service.getStatus(status.id, "service-name"))
            .isEqualToComparingFieldByField(status);
    }

    @Test
    public void should_throw_letter_not_found_exception_when_not_in_database() {
        UUID id = UUID.randomUUID();
        given(letterRepository.getLetterStatus(id, "service-name")).willReturn(Optional.empty());

        Throwable exception = catchThrowable(() -> service.getStatus(id, "service-name"));

        assertThat(exception)
            .isInstanceOf(LetterNotFoundException.class)
            .hasMessage("Letter with ID '" + id.toString() + "' not found");
    }

    @Test
    public void should_save_report_and_return_message_id_when_single_letter_is_sent() throws Exception {
        // given
        doNothing()
            .when(letterRepository)
            .save(any(DbLetter.class), any(Instant.class), anyString());

        //when
        UUID letterUuid = service.send(SampleData.letter(), "service");
        String letterId = letterUuid.toString();

        //then
        assertThat(letterId).isNotNull();

        verify(letterRepository).save(any(DbLetter.class), any(Instant.class), anyString());
    }

    @Test
    public void should_not_save_report_and_send_message_when_saving_report_fails() throws Exception {
        // given
        willThrow(CleanupFailureDataAccessException.class).given(letterRepository)
            .save(any(DbLetter.class), any(Instant.class), anyString());

        //when
        Throwable exception = catchThrowable(() -> service.send(SampleData.letter(), "service"));

        //then
        assertThat(exception).isInstanceOf(CleanupFailureDataAccessException.class);

        verify(letterRepository).save(any(DbLetter.class), any(Instant.class), anyString());
    }


    @Test
    public void should_not_allow_null_service_name() {
        assertThatThrownBy(() -> service.send(SampleData.letter(), null))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void should_not_allow_empty_service_name() {
        assertThatThrownBy(() -> service.send(SampleData.letter(), ""))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void update_should_throw_an_exception_if_no_letters_were_updated() {
        given(letterRepository.updateSentToPrintAt(any(), any())).willReturn(0);

        Throwable exc = catchThrowable(() ->
            service.updateSentToPrintAt(
                UUID.randomUUID(),
                new LetterSentToPrintAtPatch(LocalDateTime.now())
            )
        );

        assertThat(exc).isInstanceOf(LetterNotFoundException.class);
    }

    @Test
    public void update_should_pass_correct_data_to_update_database() {
        given(letterRepository.updateSentToPrintAt(any(), any())).willReturn(1);
        UUID id = UUID.randomUUID();
        LocalDateTime sentToPrintAt = LocalDateTime.now();

        // when
        service.updateSentToPrintAt(id, new LetterSentToPrintAtPatch(sentToPrintAt));

        // then
        verify(letterRepository).updateSentToPrintAt(id, sentToPrintAt);
    }

    @Test
    public void updatePrintedAt_should_throw_an_exception_if_no_letters_were_updated() {
        given(letterRepository.updatePrintedAt(any(), any())).willReturn(0);

        Throwable exc = catchThrowable(() ->
            service.updatePrintedAt(
                UUID.randomUUID(),
                new LetterPrintedAtPatch(LocalDateTime.now())
            )
        );

        assertThat(exc).isInstanceOf(LetterNotFoundException.class);
    }

    @Test
    public void updatePrintedAt_should_pass_correct_data_to_update_database() {
        given(letterRepository.updatePrintedAt(any(), any())).willReturn(1);
        UUID id = UUID.randomUUID();
        LocalDateTime dateTime = LocalDateTime.now();

        // when
        service.updatePrintedAt(id, new LetterPrintedAtPatch(dateTime));

        // then
        verify(letterRepository).updatePrintedAt(id, dateTime);
    }

    @Test
    public void updateIsFailed_should_throw_an_exception_if_no_letters_were_updated() {
        given(letterRepository.updateIsFailed(any())).willReturn(0);

        Throwable exc = catchThrowable(() -> service.updateIsFailed(UUID.randomUUID()));

        assertThat(exc).isInstanceOf(LetterNotFoundException.class);
    }

    @Test
    public void updateIsFailed_should_pass_correct_data_to_update_database() {
        given(letterRepository.updateIsFailed(any())).willReturn(1);
        UUID id = UUID.randomUUID();
        // when
        service.updateIsFailed(id);

        // then
        verify(letterRepository).updateIsFailed(id);
    }
}
