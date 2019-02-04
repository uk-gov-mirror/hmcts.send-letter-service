package uk.gov.hmcts.reform.sendletter.data.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.Location;
import org.junit.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

public class DbMigrationFilesTest {

    @Test
    public void should_have_unique_migrations() throws IOException {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        for (Location location : Flyway.configure().getLocations()) {
            String migrationLocation = location.getDescriptor();
            Resource[] resources = resolver.getResources(migrationLocation + "/*.sql");

            HashSet<String> duplicates = new HashSet<>();
            HashSet<String> testSet = new HashSet<>();

            Arrays.stream(resources)
                .map(Resource::getFilename)
                .map(filename -> filename.split("__"))
                .map(migration -> migration[0])
                .map(version -> version.substring(1))
                .forEach(version -> {
                    if (!testSet.add(version)) {
                        duplicates.add(version);
                    }
                });

            assertThat(duplicates)
                .withFailMessage("Incorrect migration script versions: %s", String.join(", ", duplicates))
                .isEmpty();
        }
    }
}
