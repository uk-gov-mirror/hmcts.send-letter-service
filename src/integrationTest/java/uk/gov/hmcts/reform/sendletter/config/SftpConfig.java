package uk.gov.hmcts.reform.sendletter.config;

import net.schmizz.sshj.SSHClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.sendletter.helper.FakeFtpAvailabilityChecker;
import uk.gov.hmcts.reform.sendletter.services.ftp.IFtpAvailabilityChecker;

import java.util.function.Supplier;

@Configuration
public class SftpConfig {

    @Bean
    public Supplier<SSHClient> sshClient() {
        // Provide clients that do not verify
        // host name and key for local testing.
        return () -> {
            SSHClient client = new SSHClient();
            client.addHostKeyVerifier((a, b, c) -> true);
            return client;
        };
    }

    @Bean()
    public IFtpAvailabilityChecker ftpAvailabilityChecker() {
        return new FakeFtpAvailabilityChecker();
    }
}
