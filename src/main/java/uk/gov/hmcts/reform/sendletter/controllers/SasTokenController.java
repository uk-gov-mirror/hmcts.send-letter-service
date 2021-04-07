package uk.gov.hmcts.reform.sendletter.controllers;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.sendletter.model.out.SasTokenResponse;
import uk.gov.hmcts.reform.sendletter.services.AuthService;
import uk.gov.hmcts.reform.sendletter.services.SasTokenGeneratorService;

import java.util.UUID;

import static uk.gov.hmcts.reform.sendletter.controllers.ControllerResponseMessage.RESPONSE_401;
import static uk.gov.hmcts.reform.sendletter.controllers.ControllerResponseMessage.RESPONSE_403;

@RestController
@RequestMapping(path = "/token")
public class SasTokenController {

    private final SasTokenGeneratorService sasTokenGeneratorService;
    private final AuthService authService;

    public SasTokenController(SasTokenGeneratorService sasTokenGeneratorService, AuthService authService) {
        this.sasTokenGeneratorService = sasTokenGeneratorService;
        this.authService = authService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get SAS Token to access blob storage and unique request id")
    @ApiResponses({
        @ApiResponse(code = 200, response = SasTokenResponse.class, message = "Successful"),
        @ApiResponse(code = 401, message = RESPONSE_401),
        @ApiResponse(code = 403, message = RESPONSE_403)
    })
    public ResponseEntity<SasTokenResponse> getSasToken(
        @RequestHeader(name = "ServiceAuthorization", required = false) String serviceAuthHeader
    ) {
        String serviceName = authService.authenticate(serviceAuthHeader);
        String sasToken = sasTokenGeneratorService.generateSasToken(serviceName);
        UUID id = UUID.randomUUID();
        return ResponseEntity.ok(new SasTokenResponse(sasToken, id));
    }
}
