package uk.gov.hmcts.reform.slc.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.slc.model.Letter;
import uk.gov.hmcts.reform.slc.services.steps.getpdf.PdfCreator;
import uk.gov.hmcts.reform.slc.services.steps.getpdf.PdfDoc;
import uk.gov.hmcts.reform.slc.services.steps.sftpupload.FtpClient;

import java.util.Objects;

import static uk.gov.hmcts.reform.slc.services.MessageHandlingResult.FAILURE;
import static uk.gov.hmcts.reform.slc.services.MessageHandlingResult.SUCCESS;

@Service
public class SendLetterService {

    private static final Logger logger = LoggerFactory.getLogger(SendLetterService.class);

    private final PdfCreator pdfCreator;
    private final FtpClient ftpClient;
    private final SendLetterClient sendLetterClient;

    public SendLetterService(
        PdfCreator pdfCreator,
        FtpClient ftpClient,
        SendLetterClient sendLetterClient
    ) {
        this.pdfCreator = pdfCreator;
        this.ftpClient = ftpClient;
        this.sendLetterClient = sendLetterClient;
    }

    public MessageHandlingResult send(Letter letter) {

        try {
            PdfDoc pdf = pdfCreator.create(letter);
            // TODO: encrypt & sign
            ftpClient.upload(pdf);

            //update producer with sent to print at time for reporting
            sendLetterClient.updateSentToPrintAt(letter.id);

            return SUCCESS;

        } catch (Exception exc) {
            logger.error("Exception occurred while processing letter ", exc);

            //update producer with is_failed status for reporting
            if (Objects.nonNull(letter)) {
                sendLetterClient.updateIsFailedStatus(letter.id);
            } else {
                logger.error("Unable to update is_failed status in producer for reporting");
            }

            return FAILURE;
        }
    }
}
