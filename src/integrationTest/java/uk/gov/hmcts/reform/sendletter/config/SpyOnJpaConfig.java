package uk.gov.hmcts.reform.sendletter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;

import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.mock;

@Configuration
public class SpyOnJpaConfig {

    @Bean
    @Primary
    public LetterRepository getLetterRepositorySpy(final LetterRepository originalLetterRepository) {
        return mock(LetterRepository.class, delegatesTo(originalLetterRepository));
    }
}
