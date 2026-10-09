package io.github.juankia.transfers.it;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityIT extends AbstractIntegrationTest {

    @Test
    void anonymousCallsAreUnauthorized() throws Exception {
        mvc.perform(post("/api/v1/transfers").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tamperedTokenIsUnauthorized() throws Exception {
        UUID account = openAccount("alice", "1");
        mvc.perform(get("/api/v1/accounts/" + account).header("Authorization", bearer("alice") + "x"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void missingScopeIsForbidden() throws Exception {
        UUID payer = openAccount("alice", "100");
        UUID payee = openAccount("bob", "0");
        mvc.perform(post("/api/v1/transfers")
                        .header("Authorization", bearer("alice", List.of("accounts:read")))
                        .header("Idempotency-Key", newKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(payer, payee, "1")))
                .andExpect(status().isForbidden());
    }

    @Test
    void cannotSpendFromSomeoneElsesAccount() throws Exception {
        UUID victim = openAccount("alice", "100");
        UUID attacker = openAccount("mallory", "0");

        postTransfer("mallory", newKey(), body(victim, attacker, "100")).andExpect(status().isForbidden());
    }

    @Test
    void cannotReadSomeoneElsesStatementOrTransfer() throws Exception {
        UUID payer = openAccount("alice", "100");
        UUID payee = openAccount("bob", "0");
        String id = read(postTransfer("alice", newKey(), body(payer, payee, "5"))).get("id").asText();

        mvc.perform(get("/api/v1/accounts/" + payer + "/statement").header("Authorization", bearer("bob")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/transfers/" + id).header("Authorization", bearer("bob")))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/transfers/" + id).header("Authorization", bearer("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void openApiDocsArePublic() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }
}
