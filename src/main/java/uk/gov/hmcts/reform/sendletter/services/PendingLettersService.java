package uk.gov.hmcts.reform.sendletter.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sendletter.entity.BasicLetterInfo;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;

import java.util.List;

@Service
public class PendingLettersService {

    private final LetterRepository repo;

    public PendingLettersService(LetterRepository repo) {
        this.repo = repo;
    }

    public List<BasicLetterInfo> getPendingLetters() {
        return repo.findPendingLetters();
    }
}
