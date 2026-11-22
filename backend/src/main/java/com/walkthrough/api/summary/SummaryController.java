package com.walkthrough.api.summary;

import com.walkthrough.api.summary.dto.SummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP layer for the summary feature.
 *
 * <p>Express equivalent:
 * <pre>
 *   router.get('/api/v1/summary', (req, res) => res.json(summaryService.getSummary()));
 * </pre>
 * {@code @RestController} = {@code @Controller} + {@code @ResponseBody}: return values are written as JSON.
 *
 * <p>Keep controllers THIN: map HTTP to a service call; no business logic or repositories here.
 */
@RestController
@RequestMapping("/api/v1/summary")
@Tag(name = "Summary", description = "Dashboard statistics")
public class SummaryController {

    private final SummaryService summaryService;

    public SummaryController(SummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @GetMapping
    @Operation(summary = "Get dashboard summary",
            description = "Aggregated counts of projects and tasks, computed live from PostgreSQL.")
    public SummaryResponse getSummary() {
        return summaryService.getSummary();
    }
}
