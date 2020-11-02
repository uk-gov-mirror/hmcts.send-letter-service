package uk.gov.hmcts.reform.sendletter.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sendletter.entity.ExceptionLetter;
import uk.gov.hmcts.reform.sendletter.entity.ExceptionRepository;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExceptionLetterServiceTest {
    @Mock
    private ExceptionRepository repository;

    @Mock
    private ExceptionLetter exceptionLetter;

    private ExceptionLetterService service;

    @BeforeEach
    void setUp() {
        service = new ExceptionLetterService(repository);
    }

    @Test
    void should_invoke_save() {
        service.save(exceptionLetter);
        verify(repository).save(eq(exceptionLetter));
    }

}
