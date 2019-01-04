package uk.gov.hmcts.reform.sendletter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.io.FileOutputStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ComponentScan(basePackages = "...", lazyInit = true)
@ContextConfiguration
@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class ApplicationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    public void should_generate_swagger_docs() throws Exception {
        byte[] specs = mvc.perform(get("/v2/api-docs"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

        FileOutputStream outputStream = new FileOutputStream("/tmp/swagger-specs.json");
        outputStream.write(specs);
        outputStream.close();
    }
}
