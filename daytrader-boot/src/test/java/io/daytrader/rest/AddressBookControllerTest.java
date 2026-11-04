package io.daytrader.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AddressBookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void listReturnsAllEntriesInLegacyHashMapOrder() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/addresses"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        JsonNode addresses = objectMapper.readTree(body).path("addresses");

        assertEquals(5, addresses.size());
        String[] entryNames = {"Entry4", "Entry5", "Entry2", "Entry3", "Entry1"};
        for (int i = 0; i < entryNames.length; i++) {
            JsonNode address = addresses.get(i);
            assertEquals(entryNames[i], address.path("entryName").asText());
            assertEquals("12345", address.path("zipCode").asText());
            assertEquals("Address", address.path("streetAddress").asText());
            assertEquals("City", address.path("city").asText());
            assertEquals("State", address.path("state").asText());
            assertEquals("Country", address.path("country").asText());
        }
        assertTrue(body.contains(
                "{\"entryName\":\"Entry4\",\"zipCode\":\"12345\",\"streetAddress\":\"Address\","
                        + "\"city\":\"City\",\"state\":\"State\",\"country\":\"Country\"}"));
    }

    @Test
    void searchReturnsMatchingEntries() throws Exception {
        MvcResult entryResult = mockMvc.perform(get("/rest/addresses/search/Entry1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        JsonNode entryAddresses = objectMapper.readTree(entryResult.getResponse().getContentAsString())
                .path("addresses");
        assertEquals(1, entryAddresses.size());
        assertEquals("Entry1", entryAddresses.get(0).path("entryName").asText());

        MvcResult prefixResult = mockMvc.perform(get("/rest/addresses/search/Entry"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        assertEquals(5, objectMapper.readTree(prefixResult.getResponse().getContentAsString())
                .path("addresses").size());

        mockMvc.perform(get("/rest/addresses/search/Nope"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string("{\"addresses\":[]}"));
    }

    @Test
    void entryLookupReturnsEmptyLegacyStatusesWithoutContentType() throws Exception {
        mockMvc.perform(get("/rest/addresses/Entry2"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().string(""))
                .andExpect(header().doesNotExist("Content-Type"));

        mockMvc.perform(get("/rest/addresses/Nope"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""))
                .andExpect(header().doesNotExist("Content-Type"));
    }
}
