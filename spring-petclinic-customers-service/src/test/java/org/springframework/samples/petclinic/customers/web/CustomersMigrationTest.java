package org.springframework.samples.petclinic.customers.web;

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
class CustomersMigrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void createsAndUpdatesOwnerAndPetAfterSeedData() throws Exception {
        String owner = """
            {"firstName":"Migration","lastName":"Demo","address":"17 Java Road",
             "city":"Spring","telephone":"1234567890"}
            """;
        JsonNode created = json.readTree(mvc.perform(post("/owners")
            .contentType(MediaType.APPLICATION_JSON).content(owner))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        int ownerId = created.get("id").asInt();
        assertThat(ownerId).isGreaterThan(10);
        mvc.perform(put("/owners/" + ownerId).contentType(MediaType.APPLICATION_JSON)
            .content(owner.replace("Spring", "Cloud"))).andExpect(status().isNoContent());
        mvc.perform(get("/owners/" + ownerId))
            .andExpect(jsonPath("$.city").value("Cloud"));

        String pet = """
            {"name":"Jakarta","birthDate":"2020-02-03","typeId":1}
            """;
        JsonNode savedPet = json.readTree(mvc.perform(post("/owners/" + ownerId + "/pets")
            .contentType(MediaType.APPLICATION_JSON).content(pet))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        int petId = savedPet.get("id").asInt();
        assertThat(petId).isGreaterThan(13);
        mvc.perform(put("/owners/" + ownerId + "/pets/" + petId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"id\":" + petId + ",\"name\":\"Brave\",\"birthDate\":\"2020-02-03\",\"typeId\":1}"))
            .andExpect(status().isNoContent());
        mvc.perform(get("/owners/" + ownerId + "/pets/" + petId))
            .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Brave"))
            .andExpect(jsonPath("$.birthDate").value("2020-02-03"))
            .andExpect(jsonPath("$.type.id").value(1));
        mvc.perform(get("/owners/" + ownerId))
            .andExpect(jsonPath("$.pets[0].id").value(petId));
    }

    @Test
    void rejectsInvalidOwnerAndKeepsSeedContracts() throws Exception {
        mvc.perform(post("/owners").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/owners/1")).andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("George"));
        mvc.perform(get("/petTypes")).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("bird"));
        mvc.perform(get("/owners/1/pets/999999")).andExpect(status().isNotFound());
    }

    @Test
    void preservesLegacyTrailingSlashAndZeroIdContracts() throws Exception {
        mvc.perform(get("/owners/1/")).andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("George"));
        mvc.perform(get("/owners/0")).andExpect(status().isOk())
            .andExpect(content().string("null"));
    }
}
