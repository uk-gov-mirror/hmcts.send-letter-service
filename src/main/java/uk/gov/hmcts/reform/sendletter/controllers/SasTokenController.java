package uk.gov.hmcts.reform.sendletter.controllers;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.sendletter.model.out.SasTokenResponse;
import uk.gov.hmcts.reform.sendletter.model.out.SendLetterResponse;

import java.util.UUID;

@RestController
@RequestMapping(path = "/token")
public class SasTokenController {

    @GetMapping(path = "/{serviceName}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get SAS Token to access blob storage and unique request id")
    @ApiResponses({
        @ApiResponse(code = 200, response = SendLetterResponse.class, message = "Successfully sent letter"),
        @ApiResponse(code = 401, message = ControllerResponseMessage.RESPONSE_401),
        @ApiResponse(code = 403, message = ControllerResponseMessage.RESPONSE_403)
    })
    public ResponseEntity<SasTokenResponse> getSasToken(@PathVariable String serviceName) {
        //TODO replace with actual calls
        String sasToken = "dummy";
        UUID id = UUID.randomUUID();
        return ResponseEntity.ok(new SasTokenResponse(sasToken, id));
    }
}
