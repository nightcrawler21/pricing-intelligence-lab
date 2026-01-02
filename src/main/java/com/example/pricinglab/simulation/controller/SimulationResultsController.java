package com.example.pricinglab.simulation.controller;

import com.example.pricinglab.simulation.dto.SimulationBreakdownRowResponse;
import com.example.pricinglab.simulation.dto.SimulationRunListItemResponse;
import com.example.pricinglab.simulation.dto.SimulationRunStatusResponse;
import com.example.pricinglab.simulation.dto.SimulationSummaryResponse;
import com.example.pricinglab.simulation.dto.SimulationTimeseriesPointResponse;
import com.example.pricinglab.simulation.service.SimulationResultsQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for querying simulation results.
 *
 * Provides read-only endpoints for viewing simulation run data,
 * including status, summary metrics, timeseries, breakdowns, and CSV export.
 */
@RestController
@Tag(name = "Simulation Results", description = "Read-only APIs for simulation result data")
public class SimulationResultsController {

    private static final Logger log = LoggerFactory.getLogger(SimulationResultsController.class);

    private final SimulationResultsQueryService queryService;

    public SimulationResultsController(SimulationResultsQueryService queryService) {
        this.queryService = queryService;
    }

    // --- Experiment-scoped endpoints ---

    @GetMapping("/api/experiments/{experimentId}/simulation-runs")
    @Operation(summary = "List simulation runs for experiment",
            description = "Returns a list of simulation runs for the specified experiment, most recent first.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved simulation runs")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ResponseEntity<List<SimulationRunListItemResponse>> listRunsForExperiment(
            @PathVariable UUID experimentId,
            @Parameter(description = "Maximum number of runs to return (default 20, max 100)")
            @RequestParam(required = false) Integer limit
    ) {
        log.debug("Listing simulation runs for experiment {}, limit={}", experimentId, limit);
        List<SimulationRunListItemResponse> runs = queryService.listRunsForExperiment(experimentId, limit);
        return ResponseEntity.ok(runs);
    }

    // --- Run-scoped endpoints ---

    @GetMapping("/api/simulation-runs/{runId}/status")
    @Operation(summary = "Get simulation run status",
            description = "Returns the current status of a simulation run.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved run status"),
            @ApiResponse(responseCode = "404", description = "Simulation run not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ResponseEntity<SimulationRunStatusResponse> getRunStatus(@PathVariable UUID runId) {
        log.debug("Getting status for simulation run {}", runId);
        SimulationRunStatusResponse status = queryService.getRunStatus(runId);
        return ResponseEntity.ok(status);
    }

    @GetMapping("/api/simulation-runs/{runId}/summary")
    @Operation(summary = "Get simulation summary metrics",
            description = "Returns aggregated metrics for CONTROL and TEST variants with deltas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved summary"),
            @ApiResponse(responseCode = "404", description = "Simulation run not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ResponseEntity<SimulationSummaryResponse> getSummary(@PathVariable UUID runId) {
        log.debug("Getting summary for simulation run {}", runId);
        SimulationSummaryResponse summary = queryService.getSummary(runId);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/api/simulation-runs/{runId}/timeseries")
    @Operation(summary = "Get simulation timeseries data",
            description = "Returns per-date metrics for CONTROL and TEST variants. " +
                    "v0 only supports groupBy=DATE. Optional filters: storeId, skuId.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved timeseries"),
            @ApiResponse(responseCode = "400", description = "Invalid groupBy parameter"),
            @ApiResponse(responseCode = "404", description = "Simulation run not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ResponseEntity<List<SimulationTimeseriesPointResponse>> getTimeseries(
            @PathVariable UUID runId,
            @Parameter(description = "Grouping dimension (v0 only supports DATE)")
            @RequestParam(required = false, defaultValue = "DATE") String groupBy,
            @Parameter(description = "Optional store filter")
            @RequestParam(required = false) UUID storeId,
            @Parameter(description = "Optional SKU filter")
            @RequestParam(required = false) UUID skuId
    ) {
        log.debug("Getting timeseries for run {}, groupBy={}, storeId={}, skuId={}",
                runId, groupBy, storeId, skuId);
        List<SimulationTimeseriesPointResponse> timeseries =
                queryService.getTimeseries(runId, groupBy, storeId, skuId);
        return ResponseEntity.ok(timeseries);
    }

    @GetMapping("/api/simulation-runs/{runId}/breakdown")
    @Operation(summary = "Get simulation breakdown data",
            description = "Returns metrics grouped by the specified dimension. " +
                    "Exactly one of by=STORE, by=SKU, or by=DATE must be provided. " +
                    "Optional filters: storeId (when by=SKU or DATE), skuId (when by=STORE or DATE).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved breakdown"),
            @ApiResponse(responseCode = "400", description = "Invalid or missing 'by' parameter"),
            @ApiResponse(responseCode = "404", description = "Simulation run not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ResponseEntity<List<SimulationBreakdownRowResponse>> getBreakdown(
            @PathVariable UUID runId,
            @Parameter(description = "Breakdown dimension: STORE, SKU, or DATE", required = true)
            @RequestParam String by,
            @Parameter(description = "Optional store filter (when by=SKU or DATE)")
            @RequestParam(required = false) UUID storeId,
            @Parameter(description = "Optional SKU filter (when by=STORE or DATE)")
            @RequestParam(required = false) UUID skuId
    ) {
        log.debug("Getting breakdown for run {}, by={}, storeId={}, skuId={}",
                runId, by, storeId, skuId);
        List<SimulationBreakdownRowResponse> breakdown =
                queryService.getBreakdown(runId, by, storeId, skuId);
        return ResponseEntity.ok(breakdown);
    }

    @GetMapping("/api/simulation-runs/{runId}/export.csv")
    @Operation(summary = "Export simulation results as CSV",
            description = "Downloads all daily simulation results as a CSV file. " +
                    "Columns: runId, experimentId, date, storeId, skuId, variant, " +
                    "basePrice, price, unitCost, units, revenue, margin")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "CSV file download"),
            @ApiResponse(responseCode = "404", description = "Simulation run not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public void exportCsv(
            @PathVariable UUID runId,
            HttpServletResponse response
    ) throws IOException {
        log.info("Exporting CSV for simulation run {}", runId);

        // Set response headers
        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"simulation-" + runId + ".csv\"");

        // Write CSV directly to response
        queryService.exportToCsv(runId, response.getWriter());
        response.flushBuffer();
    }
}
