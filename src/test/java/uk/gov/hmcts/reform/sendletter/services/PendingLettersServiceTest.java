package uk.gov.hmcts.reform.sendletter.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sendletter.SampleData;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.entity.LetterStatus;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PendingLettersServiceTest {

    @Mock
    private LetterRepository repo;

    private PendingLettersService service;

    @BeforeEach
    void setUp() {
        service = new PendingLettersService(repo);
    }

    @Test
    void should_read_letters_in_proper_status_from_repo() {
        // given
        List<Letter> noLetters = Collections.emptyList();
        given(repo.findByStatus(any())).willReturn(noLetters);

        ArgumentCaptor<LetterStatus> statusArgumentCaptor = ArgumentCaptor.forClass(LetterStatus.class);

        // when
        List<Letter> letters = service.getPendingLetters();

        // then
        assertThat(letters).isEqualTo(noLetters);
        verify(repo).findByStatus(statusArgumentCaptor.capture());
        assertThat(statusArgumentCaptor.getValue()).isEqualTo(LetterStatus.Created);
    }

    @Test
    void should_return_list_of_letters_from_repo() {
        // given
        List<Letter> letters = Collections.singletonList(SampleData.letterEntity("some service"));
        given(repo.findByStatus(any())).willReturn(letters);

        // when
        List<Letter> lettersFromDb = service.getPendingLetters();

        // then
        assertThat(lettersFromDb)
            .usingRecursiveFieldByFieldElementComparator()
            .isEqualTo(letters);
    }
}
