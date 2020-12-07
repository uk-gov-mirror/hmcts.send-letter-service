package uk.gov.hmcts.reform.sendletter.controllers;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.VersionSelector;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.entity.LetterStatus;
import uk.gov.hmcts.reform.sendletter.services.AuthService;
import uk.gov.hmcts.reform.sendletter.services.LetterService;
import uk.gov.hmcts.reform.sendletter.services.ftp.ServiceFolderMapping;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(SpringExtension.class)
@Provider("rpeSendLetterService_SendLetterController")
@PactBroker(scheme = "${PACT_BROKER_SCHEME:http}",
    host = "${PACT_BROKER_URL:localhost}",
    port = "${PACT_BROKER_PORT:80}", consumerVersionSelectors = {
    @VersionSelector(tag = "${PACT_BRANCH_NAME:Dev}")})
@Import({SendLetterProviderConfiguration.class})
public class SendLetterProviderTest {

    @Autowired
    AuthService authService;

    @Autowired
    LetterService letterService;

    @Autowired
    LetterRepository letterRepository;

    @Autowired
    ServiceFolderMapping serviceFolderMapping;

    @Autowired
    SendLetterController sendLetterController;

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @BeforeEach
    void before(PactVerificationContext context) {
        System.getProperties().setProperty("pact.verifier.publishResults", "true");
        MockMvcTestTarget testTarget = new MockMvcTestTarget();
        testTarget.setControllers(sendLetterController);
        context.setTarget(testTarget);
    }

    @State({"A valid send letter request is received"})
    public void sendValidLetter() {
        Mockito.when(authService.authenticate(anyString())).thenReturn("serviceName");
        Mockito.when(serviceFolderMapping.getFolderFor("serviceName")).thenReturn(Optional.of("serviceFolder"));
        Mockito.when(letterRepository.findByChecksumAndStatusOrderByCreatedAtDesc(anyString(),
            any(LetterStatus.class))).thenReturn(Optional.empty());
    }
}
