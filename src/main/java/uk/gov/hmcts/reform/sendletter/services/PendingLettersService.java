package uk.gov.hmcts.reform.sendletter.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sendletter.entity.BasicLetterInfo;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.entity.LetterStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static uk.gov.hmcts.reform.sendletter.tasks.UploadLettersTask.SMOKE_TEST_LETTER_TYPE;

@Service
public class PendingLettersService {

    private final LetterRepository repo;

    public PendingLettersService(LetterRepository repo) {
        this.repo = repo;
    }

    public List<BasicLetterInfo> getPendingLetters() {
        return repo.findPendingLetters();
    }

    public Stream<BasicLetterInfo> getPendingLettersCreatedBeforeTime(int before) {
        LocalDateTime localDateTime = LocalDateTime.now().minusMinutes(before);
        return repo.findByCreatedAtBeforeAndStatusAndTypeNot(localDateTime,
                LetterStatus.Created, SMOKE_TEST_LETTER_TYPE);
    }
}
