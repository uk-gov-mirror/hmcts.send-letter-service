package uk.gov.hmcts.reform.sendletter.controllers.reports;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.sendletter.entity.BasicLetterInfo;
import uk.gov.hmcts.reform.sendletter.model.out.StaleLetter;
import uk.gov.hmcts.reform.sendletter.model.out.StaleLetterResponse;
import uk.gov.hmcts.reform.sendletter.services.StaleLetterService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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


    @GetMapping(value = "download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ApiOperation("Downloads file with stale letters")
    @ApiResponses({
            @ApiResponse(code = 200, response = StaleLetterResponse.class, message = "Retrieved stale letters file"),
            @ApiResponse(code = 500, message = "Error occurred while retrieving stale letters file")
    })
    public ResponseEntity<byte[]> getFileWithStaleLetters() throws IOException {
        File csvFile = staleLetterService.getDownloadFile();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=stale-letters.csv")
                .body(Files.readAllBytes(csvFile.toPath()));
    }


    private StaleLetter mapToStaleLetter(BasicLetterInfo dbLetter) {
        return new StaleLetter(
            dbLetter.getId(),
            dbLetter.getStatus(),
            dbLetter.getService(),
            dbLetter.getCreatedAt(),
            dbLetter.getSentToPrintAt()
        );
    }
}
