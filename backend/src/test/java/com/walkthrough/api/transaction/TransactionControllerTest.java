package com.walkthrough.api.transaction;

import com.walkthrough.api.transaction.dto.TransactionStatsResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Verifies that query parameters are bound into the TransactionFilter record correctly. */
@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionQueryService queryService;

    @Test
    void bindsAllFilterParametersIntoTheRecord() throws Exception {
        when(queryService.stats(any())).thenReturn(new TransactionStatsResponse(1, 1, 0,
                BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN.negate(), BigDecimal.TEN, BigDecimal.ZERO,
                BigDecimal.TEN, BigDecimal.ZERO, null, null));

        mockMvc.perform(get("/api/v1/transactions/stats")
                        .param("type", "DEBIT")
                        .param("q", "udemy")
                        .param("from", "2022-04-01")
                        .param("to", "2023-03-31")
                        .param("minAmount", "500")
                        .param("maxAmount", "5000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1));

        ArgumentCaptor<TransactionFilter> captor = ArgumentCaptor.forClass(TransactionFilter.class);
        verify(queryService).stats(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new TransactionFilter(TransactionType.DEBIT, "udemy",
                LocalDate.of(2022, 4, 1), LocalDate.of(2023, 3, 31), new BigDecimal("500"), new BigDecimal("5000")));
    }

    @Test
    void missingParametersBecomeNulls() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/stats")).andExpect(status().isOk());

        ArgumentCaptor<TransactionFilter> captor = ArgumentCaptor.forClass(TransactionFilter.class);
        verify(queryService).stats(captor.capture());
        assertThat(captor.getValue()).isEqualTo(TransactionFilter.none());
    }

    @Test
    void invalidDateIsA400() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/stats").param("from", "01-04-2022"))
                .andExpect(status().isBadRequest());
    }
}
