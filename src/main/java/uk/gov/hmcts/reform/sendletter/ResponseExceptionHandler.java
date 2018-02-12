package uk.gov.hmcts.reform.sendletter;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.DataBinder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.gov.hmcts.reform.authorisation.exceptions.InvalidTokenException;
import uk.gov.hmcts.reform.sendletter.exception.ConnectionException;
import uk.gov.hmcts.reform.sendletter.model.errors.FieldError;
import uk.gov.hmcts.reform.sendletter.model.errors.ModelValidationError;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.springframework.http.ResponseEntity.badRequest;
import static org.springframework.http.ResponseEntity.status;

@ControllerAdvice
public class ResponseExceptionHandler extends ResponseEntityExceptionHandler {

    @InitBinder
    protected void activateDirectFieldAccess(DataBinder dataBinder) {
        dataBinder.initDirectFieldAccess();
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException exception,
        HttpHeaders headers,
        HttpStatus status,
        WebRequest request
    ) {
        List<FieldError> fieldErrors =
            exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> new FieldError(err.getField(), err.getDefaultMessage()))
                .collect(toList());

        return badRequest().body(new ModelValidationError(fieldErrors));
    }

    @ExceptionHandler(InvalidTokenException.class)
    protected ResponseEntity handleInvalidTokenException() {
        return status(HttpStatus.UNAUTHORIZED).build();
    }

    @ExceptionHandler(ConnectionException.class)
    protected ResponseEntity handleServiceBusException() {
        return status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("Exception occured while communicating with service bus");
    }

    @ExceptionHandler(JsonProcessingException.class)
    protected ResponseEntity handleJsonProcessingException() {
        return status(HttpStatus.BAD_REQUEST)
            .body("Exception occured while parsing letter contents");
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity handleInternalException() {
        return status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }


}