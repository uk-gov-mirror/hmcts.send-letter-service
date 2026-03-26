package uk.gov.hmcts.reform.sendletter;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.DataBinder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.gov.hmcts.reform.authorisation.exceptions.InvalidTokenException;
import uk.gov.hmcts.reform.sendletter.exception.DuplexException;
import uk.gov.hmcts.reform.sendletter.exception.DuplicateDocumentException;
import uk.gov.hmcts.reform.sendletter.exception.FtpDownloadException;
import uk.gov.hmcts.reform.sendletter.exception.InvalidApiKeyException;
import uk.gov.hmcts.reform.sendletter.exception.LetterNotFoundException;
import uk.gov.hmcts.reform.sendletter.exception.LetterNotStaleException;
import uk.gov.hmcts.reform.sendletter.exception.LetterSaveException;
import uk.gov.hmcts.reform.sendletter.exception.ServiceNotConfiguredException;
import uk.gov.hmcts.reform.sendletter.exception.UnableToAbortLetterException;
import uk.gov.hmcts.reform.sendletter.exception.UnableToGenerateSasTokenException;
import uk.gov.hmcts.reform.sendletter.exception.UnableToMarkLetterPostedException;
import uk.gov.hmcts.reform.sendletter.exception.UnableToMarkLetterPostedLocallyException;
import uk.gov.hmcts.reform.sendletter.exception.UnableToReprocessLetterException;
import uk.gov.hmcts.reform.sendletter.exception.UnauthenticatedException;
import uk.gov.hmcts.reform.sendletter.model.out.errors.FieldError;
import uk.gov.hmcts.reform.sendletter.model.out.errors.ModelValidationError;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.ResponseEntity.badRequest;
import static org.springframework.http.ResponseEntity.status;

/**
 * Global exception handler for the application.
 */
@ControllerAdvice
public class ResponseExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ResponseExceptionHandler.class);

    /**
     * Activates direct field access for the DataBinder.
     *
     * @param dataBinder DataBinder
     */
    @InitBinder
    protected void activateDirectFieldAccess(DataBinder dataBinder) {
        dataBinder.initDirectFieldAccess();
    }

    /**
     * Handles MethodArgumentNotValidException.
     *
     * @param exception MethodArgumentNotValidException
     * @param headers HttpHeaders
     * @param status HttpStatus
     * @param request WebRequest
     * @return ResponseEntity
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException exception,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
    ) {
        List<FieldError> fieldErrors =
            exception
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> new FieldError(err.getField(), err.getDefaultMessage()))
                .collect(toList());

        ModelValidationError error = new ModelValidationError(fieldErrors);

        log.info("Bad request: {}", error);

        return badRequest().body(error);
    }

    /**
     * Handles InvalidTokenException.
     *
     * @param exc InvalidTokenException
     * @return ResponseEntity
     */
    @ExceptionHandler(InvalidTokenException.class)
    protected ResponseEntity<Void> handleInvalidTokenException(InvalidTokenException exc) {
        log.warn(exc.getMessage(), exc);
        return status(UNAUTHORIZED).build();
    }

    /**
     * Handles LetterNotFoundException.
     *
     * @param exc LetterNotFoundException
     * @return ResponseEntity
     */
    @ExceptionHandler(LetterNotFoundException.class)
    protected ResponseEntity<Void> handleLetterNotFoundException(LetterNotFoundException exc) {
        log.warn(exc.getMessage(), exc);
        return status(NOT_FOUND).build();
    }

    /**
     * Handles JsonProcessingException.
     *
     * @return ResponseEntity
     */
    @ExceptionHandler(JsonProcessingException.class)
    protected ResponseEntity<String> handleJsonProcessingException() {
        return status(BAD_REQUEST).body("Exception occurred while parsing letter contents");
    }

    /**
     * Handles LetterSaveException.
     *
     * @return ResponseEntity
     */
    @ExceptionHandler(LetterSaveException.class)
    protected ResponseEntity<String> handleInvalidPdfException() {
        // only then pdf is actually checked hence invalid pdf message
        return status(BAD_REQUEST).body("Invalid pdf");
    }

    /**
     * Handles DuplexException.
     *
     * @param exc DuplexException
     * @return ResponseEntity
     */
    @ExceptionHandler(DuplexException.class)
    protected ResponseEntity<String> handleDuplexException(DuplexException exc) {
        // failed to parse the document
        return status(BAD_REQUEST).body(exc.getMessage());
    }

    /**
     * Handles DuplicateDocumentException.
     *
     * @param exc DuplicateDocumentException
     * @return ResponseEntity
     */
    @ExceptionHandler(DuplicateDocumentException.class)
    protected ResponseEntity<String> handleDuplicateDocumentException(DuplicateDocumentException exc) {
        return status(CONFLICT).body(exc.getMessage());
    }

    /**
     * Handles UnauthenticatedException.
     *
     * @param exc UnauthenticatedException
     * @return ResponseEntity
     */
    @ExceptionHandler(UnauthenticatedException.class)
    protected ResponseEntity<String> handleUnauthenticatedException(UnauthenticatedException exc) {
        log.warn(exc.getMessage(), exc);
        return status(UNAUTHORIZED).build();
    }

    /**
     * Handles ServiceNotConfiguredException.
     *
     * @param exc ServiceNotConfiguredException
     * @return ResponseEntity
     */
    @ExceptionHandler(ServiceNotConfiguredException.class)
    protected ResponseEntity<String> handleServiceNotConfiguredException(ServiceNotConfiguredException exc) {
        log.warn(exc.getMessage(), exc);
        return status(FORBIDDEN).body("Service not configured");
    }

    /**
     * Handles Exception.
     *
     * @param exc Exception
     * @return ResponseEntity
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<Void> handleInternalException(Exception exc) {
        log.error(exc.getMessage(), exc);
        return status(INTERNAL_SERVER_ERROR).build();
    }

    /**
     * Handles DataIntegrityViolationException.
     *
     * @param dve DataIntegrityViolationException
     * @return ResponseEntity
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleDataIntegrityViolationException(final DataIntegrityViolationException dve) {
        log.error(dve.getMessage(), dve);
        return status(CONFLICT).body("Duplicate request");
    }

    /**
     * Handles UnableToGenerateSasTokenException.
     *
     * @return ResponseEntity
     */
    @ExceptionHandler(UnableToGenerateSasTokenException.class)
    protected ResponseEntity<String> handleUnableToGenerateSasTokenException() {
        return status(INTERNAL_SERVER_ERROR).body("Exception occurred while generating SAS Token");
    }

    /**
     * Handles LetterNotStaleException.
     *
     * @param exc LetterNotStaleException
     * @return ResponseEntity
     */
    @ExceptionHandler(LetterNotStaleException.class)
    protected ResponseEntity<Void> handleLetterNotStaleException(LetterNotStaleException exc) {
        log.warn(exc.getMessage(), exc);
        return status(BAD_REQUEST).build();
    }

    /**
     * Handles InvalidApiKeyException.
     *
     * @param exc InvalidApiKeyException
     * @return ResponseEntity
     */
    @ExceptionHandler(InvalidApiKeyException.class)
    protected ResponseEntity<Void> handleInvalidApiKeyException(InvalidApiKeyException exc) {
        log.warn(exc.getMessage(), exc);
        return status(UNAUTHORIZED).build();
    }

    /**
     * Handles UnableToAbortLetterException.
     *
     * @param exc UnableToAbortLetterException
     * @return ResponseEntity
     */
    @ExceptionHandler(UnableToAbortLetterException.class)
    protected ResponseEntity<String> handleUnableToAbortLetterException(UnableToAbortLetterException exc) {
        log.warn(exc.getMessage(), exc);
        return status(BAD_REQUEST).body(exc.getMessage());
    }

    /**
     * Handles UnableToReprocessLetterException.
     *
     * @param exc UnableToReprocessLetterException
     * @return ResponseEntity
     */
    @ExceptionHandler(UnableToReprocessLetterException.class)
    protected ResponseEntity<String> handleUnableToReprocessLetterException(UnableToReprocessLetterException exc) {
        log.warn(exc.getMessage(), exc);
        return status(BAD_REQUEST).body(exc.getMessage());
    }

    /**
     * Handles UnableToMarkLetterPostedLocallyException.
     *
     * @param exc UnableToMarkLetterPostedLocallyException
     * @return ResponseEntity
     */
    @ExceptionHandler(UnableToMarkLetterPostedLocallyException.class)
    protected ResponseEntity<String> handleUnableToUnableToMarkLetterPostedLocallyException(
        UnableToMarkLetterPostedLocallyException exc
    ) {
        log.warn(exc.getMessage(), exc);
        return status(BAD_REQUEST).body(exc.getMessage());
    }

    /**
     * Handles UnableToMarkLetterPostedException.
     *
     * @param exc UnableToMarkLetterPostedException
     * @return ResponseEntity
     */
    @ExceptionHandler(UnableToMarkLetterPostedException.class)
    protected ResponseEntity<String> handleUnableToUnableToMarkLetterPostedException(
        UnableToMarkLetterPostedException exc
    ) {
        log.warn(exc.getMessage(), exc);
        return status(BAD_REQUEST).body(exc.getMessage());
    }

    /**
     * Handles FtpDownloadException.
     *
     * @param exc FtpDownloadException
     * @return ResponseEntity
     */
    @ExceptionHandler(FtpDownloadException.class)
    protected ResponseEntity<String> handleFtpDownloadException(
        FtpDownloadException exc
    ) {
        log.warn(exc.getMessage(), exc);
        return status(SERVICE_UNAVAILABLE).body(exc.getMessage());
    }
}
