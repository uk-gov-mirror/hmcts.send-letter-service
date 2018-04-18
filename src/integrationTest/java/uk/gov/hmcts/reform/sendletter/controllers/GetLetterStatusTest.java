package uk.gov.hmcts.reform.sendletter.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.sendletter.SampleData;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.model.out.LetterStatus;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ComponentScan(basePackages = "...", lazyInit = true)
@ContextConfiguration
@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
public class GetLetterStatusTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LetterRepository letterRepository;

    @MockBean
    private AuthTokenValidator tokenValidator;

    @After
    public void tearDown() {
        letterRepository.deleteAll();
    }

    @Test
    public void should_return_200_when_matching_letter_found_in_db() throws Exception {
        // given
        given(tokenValidator.getServiceName("auth-header-value")).willReturn("some-service");

        // and
        Letter letter = SampleData.letterEntity("some-service");
        letterRepository.saveAndFlush(letter);

        // when
        MvcResult result = getLetterStatus(letter.getId())
            .andExpect(status().isOk())
            .andReturn();

        // then
        String actualStatus = result.getResponse().getContentAsString();
        String expectedStatus = objectMapper.writeValueAsString(
            new LetterStatus(
                letter.getId(),
                letter.getStatus().name(),
                letter.getMessageId(),
                ZonedDateTime.ofInstant(letter.getCreatedAt().toInstant(), ZoneOffset.UTC),
                null,
                null,
                false
            )
        );

        assertThat(actualStatus).isEqualTo(expectedStatus);
    }

    @Test
    public void should_return_404_when_letter_is_not_found() throws Exception {
        getLetterStatus(UUID.randomUUID()).andExpect(status().isNotFound());
    }

    private ResultActions getLetterStatus(UUID letterId) throws Exception {
        MockHttpServletRequestBuilder request =
            get("/letters/" + letterId.toString())
                .header("ServiceAuthorization", "auth-header-value")
                .contentType(MediaType.APPLICATION_JSON);

        return mvc.perform(request);
    }
}
