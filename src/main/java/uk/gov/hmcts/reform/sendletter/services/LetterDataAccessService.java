package uk.gov.hmcts.reform.sendletter.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.entity.LetterStatus;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service to wrap data access layer automatically proxied via JPA interfaces.
 * {@code @Modifying} repo methods have to be called from  {@code @Transactional} service methods.
 * This service may serve same wrapping layer idea in case JPA -> JDBC happens.
 */
@Service
public class LetterDataAccessService {

    private final LetterRepository repository;

    public LetterDataAccessService(LetterRepository repository) {
        this.repository = repository;
    }

    /**
     * Finds letter status by id.
     * @param id letter id
     * @return letter status
     */
    public Optional<LetterStatus> findLetterStatus(UUID id) {
        return repository.findLetterStatus(id);
    }

    /**
     * Finds letter service by id.
     * @param id letter id
     * @return letter service
     */
    public Optional<String> findLetterService(UUID id) {
        return repository.findLetterService(id);
    }

    /**
     * Marks letter as posted.
     * @param id letter id
     * @param printedAt time when letter was printed
     */
    @Transactional
    public void markLetterAsPosted(UUID id, LocalDateTime printedAt) {
        repository.markLetterAsPosted(id, printedAt);
    }

    /**
     * Clears file content of letters that were created before given date and have given status.
     * @param createdBefore date
     * @param status letter status
     */
    @Transactional
    public int clearFileContent(LocalDateTime createdBefore, LetterStatus status) {
        return repository.clearFileContent(createdBefore, status);
    }
}
