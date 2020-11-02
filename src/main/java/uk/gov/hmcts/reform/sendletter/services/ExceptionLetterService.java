package uk.gov.hmcts.reform.sendletter.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sendletter.entity.ExceptionLetter;
import uk.gov.hmcts.reform.sendletter.entity.ExceptionRepository;

import java.util.Optional;
import java.util.UUID;

@Service
public class ExceptionLetterService {
    private ExceptionRepository exceptionRepository;

    public ExceptionLetterService(ExceptionRepository exceptionRepository) {
        this.exceptionRepository = exceptionRepository;
    }

    public void save(ExceptionLetter exceptionLetter) {
        exceptionRepository.save(exceptionLetter);
    }

    public Optional<ExceptionLetter> isException(UUID id) {
        return exceptionRepository.findById(id);
    }
}
