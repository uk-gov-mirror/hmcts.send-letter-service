package uk.gov.hmcts.reform.sendletter.controllers;

import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.sendletter.model.out.PendingLetter;
import uk.gov.hmcts.reform.sendletter.model.out.PendingLettersResponse;
import uk.gov.hmcts.reform.sendletter.services.PendingLettersService;

import java.util.List;

import static java.util.stream.Collectors.toList;

@RestController
public class PendingLettersController {

    private final PendingLettersService service;

    public PendingLettersController(PendingLettersService service) {
        this.service = service;
    }

    @GetMapping(path = "/pending-letters")
    @ApiOperation("Retrieves letters that were not uploaded to SFTP yet.")
    public PendingLettersResponse getPendingLetters() {
        List<PendingLetter> pendingLetters =
            service
                .getPendingLetters()
                .stream()
                .map(l -> new PendingLetter(l.getId(), l.getService(), l.getCreatedAt()))
                .collect(toList());

        return new PendingLettersResponse(pendingLetters);
    }
}
