package uk.gov.hmcts.reform.sendletter.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sendletter.entity.BasicLetterInfo;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;

import java.util.List;

import static java.time.LocalDateTime.now;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.sendletter.entity.LetterStatus.Created;

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
    void should_return_list_of_letters_from_repo() {
        // given
        List<BasicLetterInfo> letters =
            asList(
                new BasicLetterInfo(randomUUID(), "c1", "s1", Created, "t1", "f1", now(), now().minusSeconds(1)),
                new BasicLetterInfo(randomUUID(), "c2", "s2", Created, "t1", "f2", now().minusSeconds(1), now())
            );

        given(repo.findPendingLetters()).willReturn(letters);

        // when
        List<BasicLetterInfo> lettersFromDb = service.getPendingLetters();

        // then
        assertThat(lettersFromDb)
            .usingRecursiveFieldByFieldElementComparator()
            .isEqualTo(letters);
    }

}
