package com.walkthrough.api.summary;

import com.walkthrough.api.summary.dto.CountItem;
import com.walkthrough.api.summary.dto.ProjectProgress;
import com.walkthrough.api.summary.dto.SummaryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * "Slice" test: starts ONLY the web layer (controllers, JSON, exception handlers) - no DB.
 * MockMvc fires fake HTTP requests, similar to supertest in Node.
 */
@WebMvcTest(SummaryController.class)
class SummaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /** Replaces the real SummaryService bean in the Spring context with a Mockito mock. */
    @MockitoBean
    private SummaryService summaryService;

    @Test
    void returnsSummaryAsJson() throws Exception {
        when(summaryService.getSummary()).thenReturn(new SummaryResponse(
                1, 4, 1, 2, 25.0,
                List.of(new CountItem("DONE", "Done", 1)),
                List.of(new CountItem("HIGH", "High", 4)),
                List.of(new ProjectProgress(7L, "Alpha", 4, 1, 25.0)),
                Instant.parse("2026-01-15T10:00:00Z")));

        mockMvc.perform(get("/api/v1/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProjects").value(1))
                .andExpect(jsonPath("$.completionRate").value(25.0))
                .andExpect(jsonPath("$.tasksByStatus[0].label").value("Done"))
                .andExpect(jsonPath("$.projects[0].name").value("Alpha"))
                .andExpect(jsonPath("$.generatedAt").value("2026-01-15T10:00:00Z"));
    }

    @Test
    void unexpectedErrorsBecomeProblemDetails() throws Exception {
        when(summaryService.getSummary()).thenThrow(new IllegalStateException("db down"));

        mockMvc.perform(get("/api/v1/summary"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("Something went wrong. Please try again later."));
    }
}
