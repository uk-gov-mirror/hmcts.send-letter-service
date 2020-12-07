package uk.gov.hmcts.reform.sendletter.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.pdf.generator.HTMLToPDFConverter;
import uk.gov.hmcts.reform.sendletter.config.PdfConversionConfig;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.services.AuthService;
import uk.gov.hmcts.reform.sendletter.services.DuplicateLetterService;
import uk.gov.hmcts.reform.sendletter.services.ExceptionLetterService;
import uk.gov.hmcts.reform.sendletter.services.ExecusionService;
import uk.gov.hmcts.reform.sendletter.services.LetterService;
import uk.gov.hmcts.reform.sendletter.services.ftp.ServiceFolderMapping;
import uk.gov.hmcts.reform.sendletter.services.pdf.DuplexPreparator;
import uk.gov.hmcts.reform.sendletter.services.pdf.PdfCreator;
import uk.gov.hmcts.reform.sendletter.services.zip.Zipper;

@TestConfiguration
@ComponentScan(basePackageClasses = PdfCreator.class)
@Import(PdfConversionConfig.class)
public class SendLetterProviderConfiguration {

    @MockBean
    AuthService authService;

    @MockBean
    LetterRepository letterRepository;

    @MockBean
    ServiceFolderMapping serviceFolderMapping;

    @MockBean
    DuplicateLetterService duplicateLetterService;

    @MockBean
    ExceptionLetterService exceptionLetterService;

    @Bean
    @Primary
    PdfCreator pdfCreator() {
        return new PdfCreator(new DuplexPreparator(), new HTMLToPDFConverter()::convert);
    }

    @Bean
    @Primary
    LetterService letterService() {
        return new LetterService(pdfCreator(), letterRepository, new Zipper(), new ObjectMapper(),
            false, "", serviceFolderMapping,
            new ExecusionService(),
            duplicateLetterService, exceptionLetterService);
    }

    @MockBean
    AuthTokenValidator authTokenValidator;

    @Bean
    @Primary
    SendLetterController sendLetterController() {
        return new SendLetterController(letterService(), authService);

    }
}
