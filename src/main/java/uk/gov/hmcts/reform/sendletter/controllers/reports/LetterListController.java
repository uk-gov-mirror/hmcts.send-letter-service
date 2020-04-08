package uk.gov.hmcts.reform.sendletter.controllers.reports;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.sendletter.entity.BasicLetterInfo;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.model.out.LetterInfo;
import uk.gov.hmcts.reform.sendletter.model.out.LettersInfoResponse;

import java.time.LocalDate;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

@RestController
public class LetterListController {

    private final LetterRepository repository;

    public LetterListController(LetterRepository repository) {
        this.repository = repository;
    }

    @GetMapping(path = "/letters")
    public LettersInfoResponse getLetters(@RequestParam(name = "date") @DateTimeFormat(iso = DATE) LocalDate date) {

        List<BasicLetterInfo> data = repository.findCreatedAt(date);
        return new LettersInfoResponse(
            data
                .stream()
                .map(l -> new LetterInfo(
                    l.getId(),
                    l.getService(),
                    l.getStatus(),
                    l.getCreatedAt(),
                    l.getSentToPrintAt(),
                    l.getPrintedAt()
                ))
                .collect(toList())
        );
    }
}
