package uk.gov.hmcts.reform.sendletter;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.RemoteFile;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static org.apache.commons.lang.time.DateUtils.addSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.util.DateUtil.now;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@TestPropertySource("classpath:application.properties")
abstract class FunctionalTestSuite {

    @Value("${s2s-url}")
    private String s2sUrl;

    @Value("${s2s-name}")
    private String s2sName;

    @Value("${s2s-secret}")
    private String s2sSecret;

    @Value("${send-letter-service-url}")
    private String sendLetterServiceUrl;

    @Value("${ftp-hostname}")
    private String ftpHostname;

    @Value("${ftp-port}")
    private Integer ftpPort;

    @Value("${ftp-fingerprint}")
    private String ftpFingerprint;

    @Value("${ftp-target-folder}")
    String ftpTargetFolder;

    @Value("${ftp-user}")
    private String ftpUser;

    @Value("${ftp-private-key}")
    private String ftpPrivateKey;

    @Value("${ftp-public-key}")
    private String ftpPublicKey;

    @Value("${encryption.enabled}")
    Boolean isEncryptionEnabled;

    /**
     * Sign in to s2s.
     *
     * @return s2s JWT token.
     */
    String signIn() {
        Map<String, Object> params = ImmutableMap.of(
            "microservice", this.s2sName,
            "oneTimePassword", new GoogleAuthenticator().getTotpPassword(this.s2sSecret)
        );

        Response response = RestAssured
            .given()
            .relaxedHTTPSValidation()
            .baseUri(this.s2sUrl)
            .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .body(params)
            .when()
            .post("/lease")
            .andReturn();

        assertThat(response.getStatusCode()).isEqualTo(200);

        return response
            .getBody()
            .print();
    }

    String sendPrintLetterRequest(String jwt, String jsonBody) {
        return RestAssured
            .given()
            .relaxedHTTPSValidation()
            .header("ServiceAuthorization", "Bearer " + jwt)
            .header(CONTENT_TYPE, getContentType())
            .baseUri(sendLetterServiceUrl)
            .body(jsonBody.getBytes())
            .when()
            .post("/letters")
            .then()
            .statusCode(200)
            .extract()
            .body()
            .jsonPath()
            .get("letter_id");
    }

    String sampleLetterRequestJson(
        String requestBodyFilename,
        String templateFilename
    ) throws IOException, JSONException {
        String requestBody = Resources.toString(getResource(requestBodyFilename), Charsets.UTF_8);
        String template = Resources.toString(getResource(templateFilename), Charsets.UTF_8);
        JSONObject object = new JSONObject(requestBody);
        JSONArray documents = object.getJSONArray("documents");

        for (int i = 0; i < documents.length(); i++) {
            documents.getJSONObject(i).put("template", template);
        }

        return object.toString();
    }

    String samplePdfLetterRequestJson(String requestBodyFilename) throws IOException {
        String requestBody = Resources.toString(getResource(requestBodyFilename), Charsets.UTF_8);
        byte[] pdf = toByteArray(getResource("test.pdf"));

        return requestBody.replace("{{pdf}}", new String(Base64.getEncoder().encode(pdf)));
    }

    SSHClient getSshClient() throws IOException {
        SSHClient ssh = new SSHClient();

        ssh.addHostKeyVerifier(ftpFingerprint);
        ssh.connect(ftpHostname, ftpPort);

        ssh.authPublickey(
            ftpUser,
            ssh.loadKeys(ftpPrivateKey, ftpPublicKey, null)
        );

        return ssh;
    }

    ZipInputStream getZipInputStream(RemoteFile zipFile) throws IOException {
        byte[] fileContent = new byte[(int) zipFile.length()];
        zipFile.read(0, fileContent, 0, (int) zipFile.length());

        ByteArrayInputStream inputStream =
            new ByteArrayInputStream(fileContent, 0, fileContent.length);

        return new ZipInputStream(inputStream);
    }

    byte[] readAllBytes(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[10000];
        int len;

        while ((len = input.read(buffer)) > 0) {
            output.write(buffer, 0, len);
        }

        return output.toByteArray();
    }

    String getPdfFileNamePattern(String letterId) {
        return String.format(
            "%s_%s_%s.pdf",
            Pattern.quote("BPS001"),
            Pattern.quote(s2sName.replace("_", "")),
            Pattern.quote(letterId)
        );
    }

    String getFileNamePattern(String letterId) {
        String format = isEncryptionEnabled ? "%s_%s_\\d{14}_%s.pgp" : "%s_%s_\\d{14}_%s.zip";

        return String.format(
            format,
            Pattern.quote("BPS001"),
            Pattern.quote(s2sName.replace("_", "")),
            Pattern.quote(letterId)
        );
    }

    void awaitAndVerifyFileOnSftp(
        String letterId,
        BiConsumer<RemoteResourceInfo, SFTPClient> action
    ) throws IOException, InterruptedException {
        Date waitUntil = addSeconds(now(), 60);

        Boolean fileExists = false;

        try (SSHClient ssh = getSshClient()) {
            SFTPClient sftp = ssh.newSFTPClient();

            while (!(now().after(waitUntil) || fileExists)) {
                Optional<RemoteResourceInfo> matchingFile = findSingleFileOnSftp(letterId, sftp);

                if (matchingFile.isPresent()) {
                    fileExists = true;
                    action.accept(matchingFile.get(), sftp);
                } else {
                    Thread.sleep(1000);
                }
            }

            if (!fileExists) {
                throw new AssertionError(
                    "The expected file for letter '" + letterId + "' didn't appear on SFTP server"
                );
            }
        }
    }

    private Optional<RemoteResourceInfo> findSingleFileOnSftp(
        String letterId,
        SFTPClient sftp
    ) throws IOException {
        String lettersFolder = String.join("/", ftpTargetFolder, "BULKPRINT");

        List<RemoteResourceInfo> matchingFiles = sftp.ls(lettersFolder, file -> file.getName().contains(letterId));

        if (matchingFiles.size() > 1) {
            String failMessage = String.format(
                "Expected one file with name containing '%s'. Found %d",
                letterId,
                matchingFiles.size()
            );

            throw new AssertionError(failMessage);
        } else {
            return matchingFiles.stream().findFirst();
        }
    }

    void validatePdfFile(
        String letterId,
        SFTPClient sftp,
        RemoteResourceInfo sftpFile,
        int noOfDocuments
    ) {
        try (RemoteFile zipFile = sftp.open(sftpFile.getPath())) {
            PdfFile pdfFile = unzipFile(zipFile);
            assertThat(pdfFile.name).matches(getPdfFileNamePattern(letterId));

            try (PDDocument pdfDocument = PDDocument.load(pdfFile.content)) {
                assertThat(pdfDocument.getNumberOfPages()).isEqualTo(noOfDocuments);
            }
        } catch (IOException e) {
            fail("Failed to validate PDF file", e);
        }
    }

    PdfFile unzipFile(RemoteFile zipFile) throws IOException {
        try (ZipInputStream zipStream = getZipInputStream(zipFile)) {
            ZipEntry firstEntry = zipStream.getNextEntry();
            byte[] pdfContent = readAllBytes(zipStream);

            ZipEntry secondEntry = zipStream.getNextEntry();
            assertThat(secondEntry).as("second file in zip").isNull();

            String pdfName = firstEntry.getName();

            return new PdfFile(pdfName, pdfContent);
        }
    }

    static class PdfFile {
        final String name;
        final byte[] content;

        PdfFile(String name, byte[] content) {
            this.name = name;
            this.content = content;
        }
    }

    abstract String getContentType();
}
