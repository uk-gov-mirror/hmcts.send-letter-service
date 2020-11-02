package uk.gov.hmcts.reform.sendletter.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sendletter.entity.DuplicateLetter;
import uk.gov.hmcts.reform.sendletter.entity.DuplicateRepository;

import java.util.Optional;
import java.util.UUID;

@Service
public class DuplicateLetterService {
    private DuplicateRepository duplicateRepository;

    public DuplicateLetterService(DuplicateRepository duplicateRepository) {
        this.duplicateRepository = duplicateRepository;
    }

    public void save(DuplicateLetter letter) {
        duplicateRepository.save(letter);
    }

    public Optional<DuplicateLetter> isDuplicate(UUID id) {
        return duplicateRepository.findById(id);
    }
}
