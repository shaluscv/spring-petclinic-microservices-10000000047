package org.springframework.samples.petclinic.visits.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VisitsMigrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void insertsAfterSeedsAndRetainsDateAndBatchContracts() throws Exception {
        JsonNode saved = json.readTree(mvc.perform(post("/owners/1/pets/1/visits")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"date\":\"2023-11-17\",\"description\":\"Boot 3 check\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.date").value("2023-11-17"))
            .andExpect(jsonPath("$.petId").value(1))
            .andReturn().getResponse().getContentAsString());
        assertThat(saved.get("id").asInt()).isGreaterThan(4);
        mvc.perform(get("/owners/1/pets/1/visits")).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].description").value("Boot 3 check"));
        mvc.perform(get("/pets/visits").param("petId", "1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].date").value("2023-11-17"));
    }

    @Test
    void rejectsOversizedDescription() throws Exception {
        mvc.perform(post("/owners/1/pets/1/visits").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(java.util.Map.of("description", "x".repeat(8193)))))
            .andExpect(status().isBadRequest());
    }
}
