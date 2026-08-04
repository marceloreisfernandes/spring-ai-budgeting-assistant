package dev.tetel.budgeting.infrastructure.web;

import dev.tetel.budgeting.application.ListTransactionsByCategoryUseCase;
import dev.tetel.budgeting.application.PersistTransactionUseCase;
import dev.tetel.budgeting.application.TransactionOutput;
import dev.tetel.budgeting.domain.Category;
import dev.tetel.budgeting.domain.TransactionSource;
import dev.tetel.budgeting.infrastructure.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@Import(SecurityConfig.class)
class TransactionControllerTest {

    private static final String VALID_BODY = """
            {"description":"Parking payment","amountInCents":6000,"category":"AUTO"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PersistTransactionUseCase persistTransactionUseCase;

    @MockitoBean
    private ListTransactionsByCategoryUseCase listTransactionsByCategoryUseCase;

    @Test
    void createsATransactionAndReturns201() throws Exception {
        given(persistTransactionUseCase.persist(any())).willReturn(new TransactionOutput(
                "11111111-1111-1111-1111-111111111111", "Parking payment", new BigDecimal("60.00"),
                Category.AUTO, TransactionSource.REST, Instant.parse("2026-01-01T10:00:00Z")));

        mockMvc.perform(post("/api/v1/transactions")
                        .with(httpBasic("local-dev-user", "local-dev-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Parking payment"))
                .andExpect(jsonPath("$.amount").value(60.00))
                .andExpect(jsonPath("$.source").value("REST"));
    }

    @Test
    void rejectsCreationWithoutCredentials() throws Exception {
        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsInvalidBodyWith400() throws Exception {
        mockMvc.perform(post("/api/v1/transactions")
                        .with(httpBasic("local-dev-user", "local-dev-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"","amountInCents":-5}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.requestId").exists());
    }

    @Test
    void errorResponseNeverLeaksInternalDetails() throws Exception {
        mockMvc.perform(post("/api/v1/transactions")
                        .with(httpBasic("local-dev-user", "local-dev-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"","amountInCents":-5}
                                """))
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void listsTransactionsAndReturns200() throws Exception {
        given(listTransactionsByCategoryUseCase.list(any())).willReturn(List.of(new TransactionOutput(
                "22222222-2222-2222-2222-222222222222", "Groceries", new BigDecimal("25.50"),
                Category.GROCERIES, TransactionSource.REST, Instant.parse("2026-01-02T10:00:00Z"))));

        mockMvc.perform(get("/api/v1/transactions")
                        .with(httpBasic("local-dev-user", "local-dev-password"))
                        .param("category", "GROCERIES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Groceries"));
    }

    @Test
    void listReturnsEmptyArrayWhenNoTransactionsExist() throws Exception {
        given(listTransactionsByCategoryUseCase.list(any())).willReturn(List.of());

        mockMvc.perform(get("/api/v1/transactions")
                        .with(httpBasic("local-dev-user", "local-dev-password"))
                        .param("category", "PHARMA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void rejectsListingWithInvalidCategory() throws Exception {
        mockMvc.perform(get("/api/v1/transactions")
                        .with(httpBasic("local-dev-user", "local-dev-password"))
                        .param("category", "NOT_A_CATEGORY"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsListingWithoutCredentials() throws Exception {
        mockMvc.perform(get("/api/v1/transactions").param("category", "AUTO"))
                .andExpect(status().isUnauthorized());
    }
}
