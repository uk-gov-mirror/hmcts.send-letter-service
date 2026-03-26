package uk.gov.hmcts.reform.sendletter.controllers;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.sendletter.exception.InvalidApiKeyException;
import uk.gov.hmcts.reform.sendletter.model.out.CheckPostedTaskResponse;
import uk.gov.hmcts.reform.sendletter.model.out.PostedReportTaskResponse;
import uk.gov.hmcts.reform.sendletter.services.CheckLettersPostedService;
import uk.gov.hmcts.reform.sendletter.services.MarkLettersPostedService;

import java.util.List;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@RestController
@RequestMapping("/tasks")
@Slf4j
public class TaskController {

    final String apiKey;
    final MarkLettersPostedService markLettersPostedService;
    final CheckLettersPostedService checkLettersPostedService;

    public TaskController(@Value("${actions.api-key}") String apiKey,
                          MarkLettersPostedService markLettersPostedService,
                          CheckLettersPostedService checkLettersPostedService) {
        this.apiKey = apiKey;
        this.markLettersPostedService = markLettersPostedService;
        this.checkLettersPostedService = checkLettersPostedService;
    }

    /**
     * Kicks off report processing in the Mark Letters Posted Service.
     *
     * @param authHeader Auth header from the request containing the API key
     * @return a {@link List} of {@link PostedReportTaskResponse} records
     */
    @GetMapping("/process-reports")
    public ResponseEntity<List<PostedReportTaskResponse>> processReports(
        @RequestHeader(value = AUTHORIZATION, required = false) String authHeader) {
        validateAuthorization(authHeader);

        List<PostedReportTaskResponse> response = markLettersPostedService.processReports();

        if (response.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Kicks of the letter check in the Check Letters Posted Service.
     *
     * @param authHeader Auth header from the request containing the API key
     * @return a {@link CheckPostedTaskResponse}
     */
    @GetMapping("/check-posted")
    public ResponseEntity<CheckPostedTaskResponse> checkPosted(
        @RequestHeader(value = AUTHORIZATION, required = false) String authHeader) {
        validateAuthorization(authHeader);

        CheckPostedTaskResponse response = checkLettersPostedService.checkLetters();

        return ResponseEntity.ok(response);
    }

    /**
     * Validate the authorization header.
     *
     * @param authorizationKey The authorization header
     */
    private void validateAuthorization(String authorizationKey) {

        if (StringUtils.isEmpty(authorizationKey)) {
            log.error("API Key is missing");
            throw new InvalidApiKeyException("API Key is missing");
        } else if (!authorizationKey.equals("Bearer " + apiKey)) {
            log.error("Invalid API Key");
            throw new InvalidApiKeyException("Invalid API Key");
        }
    }
}
