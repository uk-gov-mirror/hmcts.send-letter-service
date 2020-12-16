package uk.gov.hmcts.reform.sendletter.controllers;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.sendletter.exception.LetterNotFoundException;
import uk.gov.hmcts.reform.sendletter.model.in.LetterRequest;
import uk.gov.hmcts.reform.sendletter.model.in.LetterWithPdfsAndNumberOfCopiesRequest;
import uk.gov.hmcts.reform.sendletter.model.in.LetterWithPdfsRequest;
import uk.gov.hmcts.reform.sendletter.model.out.LetterStatus;
import uk.gov.hmcts.reform.sendletter.model.out.SendLetterResponse;
import uk.gov.hmcts.reform.sendletter.model.out.v2.LetterStatusV2;
import uk.gov.hmcts.reform.sendletter.services.AuthService;
import uk.gov.hmcts.reform.sendletter.services.LetterService;

import java.util.UUID;
import javax.validation.Valid;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@Validated
@RequestMapping(
    path = "/letters",
    produces = {MediaType.APPLICATION_JSON_VALUE}
)
public class SendLetterController {

    private final LetterService letterService;
    private final AuthService authService;

    public SendLetterController(
        LetterService letterService,
        AuthService authService
    ) {
        this.letterService = letterService;
        this.authService = authService;
    }

    @PostMapping(
        consumes = {MediaType.APPLICATION_JSON_VALUE, MediaTypes.LETTER_V1},
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiOperation(value = "Send letter to print and post service")
    @ApiResponses({
        @ApiResponse(code = 200, response = SendLetterResponse.class, message = "Successfully sent letter"),
        @ApiResponse(code = 401, message = ControllerResponseMessage.RESPONSE_401),
        @ApiResponse(code = 403, message = ControllerResponseMessage.RESPONSE_403)
    })
    public ResponseEntity<SendLetterResponse> sendLetter(
        @RequestHeader(name = "ServiceAuthorization", required = false) String serviceAuthHeader,
        @RequestParam(name = "isAsync", defaultValue = "false") String isAsync,
        @ApiParam(value = "Letter consisting of documents and type", required = true)
        @Valid @RequestBody LetterRequest letter
    ) {
        String serviceName = authService.authenticate(serviceAuthHeader);
        UUID letterId = letterService.save(letter, serviceName, isAsync);

        return ok().body(new SendLetterResponse(letterId));
    }

    @PostMapping(consumes = MediaTypes.LETTER_V2, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Send letter to print and post service")
    @ApiResponses({
        @ApiResponse(code = 200, response = SendLetterResponse.class, message = "Successfully sent letter"),
        @ApiResponse(code = 401, message = ControllerResponseMessage.RESPONSE_401),
        @ApiResponse(code = 403, message = ControllerResponseMessage.RESPONSE_403)
    })
    public ResponseEntity<SendLetterResponse> sendLetter(
        @RequestHeader(name = "ServiceAuthorization", required = false) String serviceAuthHeader,
        @RequestParam(name = "isAsync", defaultValue = "false") String isAsync,
        @ApiParam(value = "Letter consisting of documents and type", required = true)
        @Valid @RequestBody LetterWithPdfsRequest letter
    ) {
        String serviceName = authService.authenticate(serviceAuthHeader);
        UUID letterId = letterService.save(letter, serviceName, isAsync);

        return ok().body(new SendLetterResponse(letterId));
    }

    @PostMapping(consumes = MediaTypes.LETTER_V3, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Send letter to print and post service")
    @ApiResponses({
        @ApiResponse(code = 200, response = SendLetterResponse.class, message = "Successfully sent letter"),
        @ApiResponse(code = 401, message = ControllerResponseMessage.RESPONSE_401),
        @ApiResponse(code = 403, message = ControllerResponseMessage.RESPONSE_403)
    })
    public ResponseEntity<SendLetterResponse> sendLetter(
        @RequestHeader(name = "ServiceAuthorization", required = false) String serviceAuthHeader,
        @RequestParam(name = "isAsync", defaultValue = "false") String isAsync,
        @Valid @RequestBody LetterWithPdfsAndNumberOfCopiesRequest letter
    ) {
        String serviceName = authService.authenticate(serviceAuthHeader);
        UUID letterId = letterService.save(letter, serviceName, isAsync);

        return ok().body(new SendLetterResponse(letterId));
    }

    @GetMapping(path = "/{id}")
    @ApiOperation(value = "Get letter status")
    @ApiResponses({
        @ApiResponse(code = 200, response = LetterStatus.class, message = "Success"),
        @ApiResponse(code = 404, message = "Letter not found")
    })
    public ResponseEntity<LetterStatus> getLetterStatus(
        @PathVariable String id,
        @RequestParam(name = "include-additional-info", defaultValue = "false") String isAdditionalInfoRequired,
        @RequestParam(name = "check-duplicate", defaultValue = "false") String isDuplicate
    ) {
        LetterStatus letterStatus = letterService.getStatus(getLetterIdFromString(id),
                isAdditionalInfoRequired, isDuplicate);
        return ok(letterStatus);
    }

    @GetMapping(path = "/v2/{id}")
    @ApiOperation(value = "Get letter status with copies requested")
    @ApiResponses({
        @ApiResponse(code = 200, response = LetterStatus.class, message = "Success"),
        @ApiResponse(code = 404, message = "Letter not found")
    })
    public ResponseEntity<LetterStatusV2> getLatestLetterStatus(
        @PathVariable String id
    ) {
        LetterStatusV2 letterStatus =
                letterService.getLatestStatus(getLetterIdFromString(id));
        return ok(letterStatus);
    }

    private UUID getLetterIdFromString(String letterId) {
        try {
            return UUID.fromString(letterId);
        } catch (IllegalArgumentException exception) {
            throw new LetterNotFoundException(letterId, exception);
        }
    }
}
