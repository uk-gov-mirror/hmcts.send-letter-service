package uk.gov.hmcts.reform.sendletter.controllers;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.model.out.StaleLetter;
import uk.gov.hmcts.reform.sendletter.model.out.StaleLetterResponse;
import uk.gov.hmcts.reform.sendletter.services.StaleLetterService;

import java.util.List;

import static java.util.stream.Collectors.toList;

@RestController
@RequestMapping(path = "/stale-letters")
public class StaleLetterController {

    private final StaleLetterService staleLetterService;

    public StaleLetterController(StaleLetterService staleLetterService) {
        this.staleLetterService = staleLetterService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation("Retrieves stale letters")
    @ApiResponses({
        @ApiResponse(code = 200, response = StaleLetterResponse.class, message = "Retrieved stale letters"),
        @ApiResponse(code = 500, message = "Error occurred while retrieving stale letters")
    })
    public StaleLetterResponse getStaleLetters() {
        List<StaleLetter> staleLetters =
            staleLetterService
                .getStaleLetters()
                .stream()
                .map(this::mapToStaleLetter)
                .collect(toList());

        return new StaleLetterResponse(staleLetters);
    }

    private StaleLetter mapToStaleLetter(Letter dbLetter) {
        return new StaleLetter(
            dbLetter.getId(),
            dbLetter.getStatus().name(),
            dbLetter.getService(),
            dbLetter.getCreatedAt(),
            dbLetter.getSentToPrintAt(),
            dbLetter.isFailed()
        );
    }
}
