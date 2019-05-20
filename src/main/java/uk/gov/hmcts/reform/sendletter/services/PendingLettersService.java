package uk.gov.hmcts.reform.sendletter.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;

import java.util.List;

import static uk.gov.hmcts.reform.sendletter.entity.LetterStatus.Created;

@Service
public class PendingLettersService {

    private final LetterRepository repo;

    public PendingLettersService(LetterRepository repo) {
        this.repo = repo;
    }

    public List<Letter> getPendingLetters() {
        return repo.findByStatus(Created);
    }
}
