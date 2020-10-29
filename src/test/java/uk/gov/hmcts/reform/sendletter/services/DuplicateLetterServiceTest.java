package uk.gov.hmcts.reform.sendletter.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sendletter.entity.DuplicateLetter;
import uk.gov.hmcts.reform.sendletter.entity.DuplicateRepository;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DuplicateLetterServiceTest {

    @Mock DuplicateRepository duplicateRepository;
    @Mock DuplicateLetter duplicateLetter;
    DuplicateLetterService duplicateLetterService;

    @BeforeEach
    void setUp() {
        duplicateLetterService = new DuplicateLetterService(duplicateRepository);
    }

    @Test
    void should_invoke_save() {
        duplicateLetterService.save(duplicateLetter);
        verify(duplicateRepository).save(eq(duplicateLetter));
    }


}