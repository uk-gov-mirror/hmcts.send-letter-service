package uk.gov.hmcts.reform.sendletter.config;

import net.schmizz.sshj.SSHClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.sendletter.services.ftp.FtpAvailabilityChecker;
import uk.gov.hmcts.reform.sendletter.services.ftp.IFtpAvailabilityChecker;

import java.util.function.Supplier;

@Configuration
public class SftpConfig {

    @Bean
    public Supplier<SSHClient> sshClient() {
        return SSHClient::new;
    }

    @Bean
    public IFtpAvailabilityChecker ftpAvailabilityChecker(
        @Value("${ftp.downtime.from}") String downtimeFromHour,
        @Value("${ftp.downtime.to}") String downtimeToHour,
        @Value("${ftp.disableDowntime}") boolean disableDowntime
    ) {
        return new FtpAvailabilityChecker(downtimeFromHour, downtimeToHour,disableDowntime);
    }
}
