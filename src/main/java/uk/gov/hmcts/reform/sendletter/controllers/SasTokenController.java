package uk.gov.hmcts.reform.sendletter.controllers;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.http.HttpResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.sendletter.model.out.SasTokenResponse;
import uk.gov.hmcts.reform.sendletter.model.out.SendLetterResponse;

import java.util.UUID;

import static uk.gov.hmcts.reform.sendletter.controllers.ControllerResponseMessage.*;

@RestController
@RequestMapping(path = "/token")
public class SasTokenController {

    @GetMapping(path = "/{serviceName}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get SAS Token to access blob storage and unique request id")
    @ApiResponses({
        @ApiResponse(code = 200, response = SasTokenResponse.class, message = "Successful"),
        @ApiResponse(code = 401, message = RESPONSE_401),
        @ApiResponse(code = 403, message = RESPONSE_403)
    })
    public ResponseEntity<SasTokenResponse> getSasToken(@PathVariable String serviceName) {
        //TODO replace with actual calls
        String sasToken = "dummy";
        UUID id = UUID.randomUUID();
        return ResponseEntity.ok(new SasTokenResponse(sasToken, id));
    }
}
