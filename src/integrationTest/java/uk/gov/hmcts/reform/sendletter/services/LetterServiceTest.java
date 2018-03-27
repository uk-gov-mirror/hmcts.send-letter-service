package uk.gov.hmcts.reform.sendletter.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.preflight.PreflightDocument;
import org.apache.pdfbox.preflight.parser.PreflightParser;
import org.apache.pdfbox.preflight.utils.ByteArrayDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sendletter.SampleData;
import uk.gov.hmcts.reform.sendletter.config.SpyOnJpaConfig;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.entity.LetterState;
import uk.gov.hmcts.reform.sendletter.model.in.LetterRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;
import javax.activation.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
@ImportAutoConfiguration(SpyOnJpaConfig.class)
public class LetterServiceTest {

    private static final String SERVICE_NAME = "a_service";

    private LetterService service;

    @Autowired
    private LetterRepository letterRepository;

    @Before
    public void setUp() {
        service = new LetterService(letterRepository, new ObjectMapper());
    }

    @After
    public void tearDown() {
        reset(letterRepository);
    }

    @Test
    public void generates_and_saves_pdf() throws IOException {
        UUID id = service.send(SampleData.letter(), SERVICE_NAME);
        Letter result = letterRepository.findOne(id);
        DataSource dataSource = new ByteArrayDataSource(new ByteArrayInputStream(result.getPdf()));
        PreflightParser pdfParser = new PreflightParser(dataSource);
        pdfParser.parse();
        PreflightDocument document = pdfParser.getPreflightDocument();
        // This will throw an exception if the file format is invalid,
        // but ignores more minor errors such as missing metadata.
        document.validate();
    }

    @Test
    public void generate_and_save_pdf_and_return_same_id_on_resubmit() throws IOException {
        // given
        LetterRequest sampleRequest = SampleData.letter();
        UUID id1 = service.send(sampleRequest, SERVICE_NAME);
        Letter letter = letterRepository.findOne(id1);

        // and
        assertThat(letter.getState()).isEqualByComparingTo(LetterState.Created);

        // when
        UUID id2 = service.send(sampleRequest, SERVICE_NAME);

        // then
        assertThat(id1).isEqualByComparingTo(id2);

        // and
        verify(letterRepository).save(any(Letter.class));
    }

    @Test
    public void generate_and_save_pdf_twice_if_previous_letter_has_been_sent_to_print() throws IOException {
        // given
        LetterRequest sampleRequest = SampleData.letter();
        UUID id1 = service.send(sampleRequest, SERVICE_NAME);
        Letter letter = letterRepository.findOne(id1);

        // and
        assertThat(letter.getState()).isEqualByComparingTo(LetterState.Created);

        // when
        letter.setState(LetterState.Uploaded);
        letterRepository.saveAndFlush(letter);
        UUID id2 = service.send(sampleRequest, SERVICE_NAME);

        // then
        assertThat(id1).isNotEqualByComparingTo(id2);

        // and
        verify(letterRepository, times(2)).save(any(Letter.class));
    }

    @Test
    public void should_not_allow_null_service_name() {
        assertThatThrownBy(() -> service.send(SampleData.letter(), null))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void should_not_allow_empty_service_name() {
        assertThatThrownBy(() -> service.send(SampleData.letter(), ""))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void handles_null_timestamps() {
        assertThat(LetterService.toDateTime(null)).isNull();
    }
}
