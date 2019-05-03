package uk.gov.hmcts.reform.sendletter.controllers;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.sendletter.exception.CsvReportGenerationException;
import uk.gov.hmcts.reform.sendletter.model.out.LettersCountSummary;
import uk.gov.hmcts.reform.sendletter.model.out.SendLetterResponse;
import uk.gov.hmcts.reform.sendletter.services.ReportsService;
import uk.gov.hmcts.reform.sendletter.util.CsvWriter;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

@RestController
@RequestMapping(path = "/reports")
public class ReportsController {

    private final ReportsService reportsService;

    public ReportsController(ReportsService reportsService) {
        this.reportsService = reportsService;
    }

    @GetMapping(path = "/count-summary", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ApiOperation("Retrieves uploaded letters count summary report")
    @ApiResponses({
        @ApiResponse(code = 200, response = SendLetterResponse.class, message = "Successfully generated csv report"),
        @ApiResponse(code = 500, message = "Error occurred while generating csv report")
    })
    public ResponseEntity<byte[]> getCountSummary(
        @RequestParam(name = "date") @DateTimeFormat(iso = DATE) LocalDate date
    ) {
        try {
            List<LettersCountSummary> countSummary = reportsService.getCountFor(date);
            File csvFile = CsvWriter.writeLettersCountSummaryToCsv(countSummary);
            return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=letters-count-summary.csv")
                .body(Files.readAllBytes(csvFile.toPath()));
        } catch (Exception e) {
            throw new CsvReportGenerationException(e);
        }
    }

}
