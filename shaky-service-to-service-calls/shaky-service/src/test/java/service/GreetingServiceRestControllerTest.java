package service ;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import service.GreetingApplication;

import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = GreetingApplication.class, webEnvironment = MOCK)
public class GreetingServiceRestControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Before
    public void before() throws Exception {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();
    }

    @Test
    public void greetings() throws Exception {
        String cnj = "CNJ";
        this.mockMvc
                .perform(get("/hi/" + cnj).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(
                        mvcResult -> {
                            String json = mvcResult.getResponse().getContentAsString();
                            TypeReference<Map<String, String>> typeReference =
                                    new TypeReference<Map<String, String>>() {
                                    };
                            Map<String, String> mapOfData = objectMapper.readerFor(typeReference)
                                    .readValue(json.getBytes());
                            assertTrue(mapOfData.get("greeting").contains(cnj));
                        });
    }
}