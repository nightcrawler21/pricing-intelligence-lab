package com.example.pricinglab.simulation.service;

import com.example.pricinglab.common.exception.ResourceNotFoundException;
import com.example.pricinglab.simulation.domain.SimulationResultDaily;
import com.example.pricinglab.simulation.domain.SimulationRun;
import com.example.pricinglab.simulation.dto.DeltaMetrics;
import com.example.pricinglab.simulation.dto.SimulationBreakdownRowResponse;
import com.example.pricinglab.simulation.dto.SimulationRunListItemResponse;
import com.example.pricinglab.simulation.dto.SimulationRunStatusResponse;
import com.example.pricinglab.simulation.dto.SimulationSummaryResponse;
import com.example.pricinglab.simulation.dto.SimulationTimeseriesPointResponse;
import com.example.pricinglab.simulation.dto.VariantMetrics;
import com.example.pricinglab.simulation.repository.SimulationResultDailyRepository;
import com.example.pricinglab.simulation.repository.SimulationResultDailyRepository.DateVariantAggregate;
import com.example.pricinglab.simulation.repository.SimulationResultDailyRepository.SkuVariantAggregate;
import com.example.pricinglab.simulation.repository.SimulationResultDailyRepository.StoreVariantAggregate;
import com.example.pricinglab.simulation.repository.SimulationResultDailyRepository.VariantAggregate;
import com.example.pricinglab.simulation.repository.SimulationRunRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for querying simulation results.
 * Provides read-only operations for simulation run data.
 */
@Service
@Transactional(readOnly = true)
public class SimulationResultsQueryService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final SimulationRunRepository runRepository;
    private final SimulationResultDailyRepository resultRepository;

    public SimulationResultsQueryService(
            SimulationRunRepository runRepository,
            SimulationResultDailyRepository resultRepository) {
        this.runRepository = runRepository;
        this.resultRepository = resultRepository;
    }

    /**
     * Lists simulation runs for an experiment, most recent first.
     *
     * @param experimentId the experiment ID
     * @param limit maximum number of runs to return (default 20, max 100)
     * @return list of simulation run summaries
     */
    public List<SimulationRunListItemResponse> listRunsForExperiment(UUID experimentId, Integer limit) {
        int effectiveLimit = resolveLimit(limit);

        List<SimulationRun> runs = runRepository.findByExperimentIdWithLimit(
                experimentId,
                PageRequest.of(0, effectiveLimit)
        );

        return runs.stream()
                .map(this::toListItemResponse)
                .toList();
    }

    /**
     * Gets the status of a simulation run.
     *
     * @param runId the simulation run ID
     * @return status response
     * @throws ResourceNotFoundException if run not found
     */
    public SimulationRunStatusResponse getRunStatus(UUID runId) {
        SimulationRun run = findRunOrThrow(runId);
        return new SimulationRunStatusResponse(
                run.getId(),
                run.getExperiment().getId(),
                run.getStatus(),
                run.getStartedAt(),
                run.getCompletedAt(),
                run.getErrorMessage()
        );
    }

    /**
     * Gets summary metrics for a simulation run.
     *
     * @param runId the simulation run ID
     * @return summary with control/test/delta metrics
     * @throws ResourceNotFoundException if run not found
     */
    public SimulationSummaryResponse getSummary(UUID runId) {
        findRunOrThrow(runId);

        List<VariantAggregate> aggregates = resultRepository.aggregateTotalsByVariant(runId);

        VariantMetrics control = VariantMetrics.zero();
        VariantMetrics test = VariantMetrics.zero();

        for (VariantAggregate agg : aggregates) {
            VariantMetrics metrics = new VariantMetrics(
                    agg.getTotalUnits(),
                    agg.getTotalRevenue(),
                    agg.getTotalMargin()
            );
            if (agg.isTestGroup()) {
                test = metrics;
            } else {
                control = metrics;
            }
        }

        DeltaMetrics delta = calculateDelta(control, test);

        return new SimulationSummaryResponse(runId, control, test, delta);
    }

    /**
     * Gets timeseries data for a simulation run.
     *
     * @param runId the simulation run ID
     * @param groupBy grouping dimension (only DATE supported in v0)
     * @param storeId optional store filter
     * @param skuId optional SKU filter
     * @return list of timeseries points sorted by date
     * @throws ResourceNotFoundException if run not found
     * @throws IllegalArgumentException if groupBy is not DATE
     */
    public List<SimulationTimeseriesPointResponse> getTimeseries(
            UUID runId, String groupBy, UUID storeId, UUID skuId) {

        findRunOrThrow(runId);
        validateTimeseriesGroupBy(groupBy);

        List<DateVariantAggregate> aggregates = fetchTimeseriesAggregates(runId, storeId, skuId);

        return buildTimeseriesResponse(aggregates);
    }

    /**
     * Gets breakdown data for a simulation run.
     *
     * @param runId the simulation run ID
     * @param by breakdown dimension (STORE, SKU, or DATE)
     * @param storeId optional store filter
     * @param skuId optional SKU filter
     * @return list of breakdown rows
     * @throws ResourceNotFoundException if run not found
     * @throws IllegalArgumentException if by param is invalid
     */
    public List<SimulationBreakdownRowResponse> getBreakdown(
            UUID runId, String by, UUID storeId, UUID skuId) {

        findRunOrThrow(runId);
        validateBreakdownBy(by);

        return switch (by.toUpperCase()) {
            case "STORE" -> buildStoreBreakdown(runId, skuId);
            case "SKU" -> buildSkuBreakdown(runId, storeId);
            case "DATE" -> buildDateBreakdown(runId, storeId, skuId);
            default -> throw new IllegalArgumentException(
                    "Invalid 'by' parameter: " + by + ". Must be one of: STORE, SKU, DATE");
        };
    }

    /**
     * Writes CSV export data for a simulation run.
     *
     * @param runId the simulation run ID
     * @param writer the writer to write CSV to
     * @throws ResourceNotFoundException if run not found
     * @throws IOException if write fails
     */
    public void exportToCsv(UUID runId, Writer writer) throws IOException {
        SimulationRun run = findRunOrThrow(runId);
        UUID experimentId = run.getExperiment().getId();

        List<SimulationResultDaily> results = resultRepository.findByRunIdOrderedForExport(runId);

        // Write header
        writer.write("runId,experimentId,date,storeId,skuId,variant,basePrice,price,unitCost,units,revenue,margin\n");

        // Write data rows
        for (SimulationResultDaily result : results) {
            writer.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                    runId,
                    experimentId,
                    result.getSimulationDate(),
                    result.getStore().getId(),
                    result.getSku().getId(),
                    result.isTestGroup() ? "TEST" : "CONTROL",
                    result.getBasePrice(),
                    result.getSimulatedPrice(),
                    result.getUnitCost() != null ? result.getUnitCost() : "",
                    result.getProjectedUnits().setScale(0, RoundingMode.HALF_UP),
                    result.getProjectedRevenue(),
                    result.getProjectedMargin()
            ));
        }
    }

    /**
     * Gets the count of result rows for a run (for CSV Content-Length hint).
     */
    public long getResultCount(UUID runId) {
        findRunOrThrow(runId);
        return resultRepository.findBySimulationRunId(runId).size();
    }

    // --- Private helper methods ---

    private SimulationRun findRunOrThrow(UUID runId) {
        return runRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("SimulationRun", runId.toString()));
    }

    private int resolveLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private SimulationRunListItemResponse toListItemResponse(SimulationRun run) {
        return new SimulationRunListItemResponse(
                run.getId(),
                run.getStatus(),
                run.getStartedAt(),
                run.getCompletedAt(),
                run.getTotalDaysSimulated(),
                run.getErrorMessage()
        );
    }

    private DeltaMetrics calculateDelta(VariantMetrics control, VariantMetrics test) {
        BigDecimal deltaUnits = test.units().subtract(control.units());
        BigDecimal deltaRevenue = test.revenue().subtract(control.revenue());
        BigDecimal deltaMargin = test.margin().subtract(control.margin());

        BigDecimal revenuePct = calculatePercentChange(control.revenue(), test.revenue());
        BigDecimal marginPct = calculatePercentChange(control.margin(), test.margin());

        return new DeltaMetrics(deltaUnits, deltaRevenue, deltaMargin, revenuePct, marginPct);
    }

    private BigDecimal calculatePercentChange(BigDecimal baseline, BigDecimal newValue) {
        if (baseline.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO; // Handle divide-by-zero
        }
        return newValue.subtract(baseline)
                .divide(baseline, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    private void validateTimeseriesGroupBy(String groupBy) {
        if (groupBy == null || groupBy.isBlank()) {
            return; // Default to DATE
        }
        if (!groupBy.equalsIgnoreCase("DATE")) {
            throw new IllegalArgumentException(
                    "Invalid 'groupBy' parameter: " + groupBy + ". v0 only supports: DATE");
        }
    }

    private void validateBreakdownBy(String by) {
        if (by == null || by.isBlank()) {
            throw new IllegalArgumentException(
                    "Missing required 'by' parameter. Must be one of: STORE, SKU, DATE");
        }
        String upper = by.toUpperCase();
        if (!upper.equals("STORE") && !upper.equals("SKU") && !upper.equals("DATE")) {
            throw new IllegalArgumentException(
                    "Invalid 'by' parameter: " + by + ". Must be one of: STORE, SKU, DATE");
        }
    }

    private List<DateVariantAggregate> fetchTimeseriesAggregates(UUID runId, UUID storeId, UUID skuId) {
        if (storeId != null && skuId != null) {
            return resultRepository.aggregateByDateAndVariantForStoreAndSku(runId, storeId, skuId);
        } else if (storeId != null) {
            return resultRepository.aggregateByDateAndVariantForStore(runId, storeId);
        } else if (skuId != null) {
            return resultRepository.aggregateByDateAndVariantForSku(runId, skuId);
        } else {
            return resultRepository.aggregateByDateAndVariant(runId);
        }
    }

    private List<SimulationTimeseriesPointResponse> buildTimeseriesResponse(List<DateVariantAggregate> aggregates) {
        // Group by date, then combine CONTROL + TEST into single response
        Map<LocalDate, VariantMetrics[]> byDate = new LinkedHashMap<>();

        for (DateVariantAggregate agg : aggregates) {
            LocalDate date = agg.getSimulationDate();
            VariantMetrics[] pair = byDate.computeIfAbsent(date, k -> new VariantMetrics[2]);

            VariantMetrics metrics = new VariantMetrics(
                    agg.getTotalUnits(),
                    agg.getTotalRevenue(),
                    agg.getTotalMargin()
            );

            if (agg.isTestGroup()) {
                pair[1] = metrics; // TEST
            } else {
                pair[0] = metrics; // CONTROL
            }
        }

        List<SimulationTimeseriesPointResponse> result = new ArrayList<>();
        for (Map.Entry<LocalDate, VariantMetrics[]> entry : byDate.entrySet()) {
            VariantMetrics control = entry.getValue()[0] != null ? entry.getValue()[0] : VariantMetrics.zero();
            VariantMetrics test = entry.getValue()[1] != null ? entry.getValue()[1] : VariantMetrics.zero();
            DeltaMetrics delta = calculateDelta(control, test);
            result.add(new SimulationTimeseriesPointResponse(entry.getKey(), control, test, delta));
        }

        return result;
    }

    private List<SimulationBreakdownRowResponse> buildStoreBreakdown(UUID runId, UUID skuId) {
        List<StoreVariantAggregate> aggregates = (skuId != null)
                ? resultRepository.aggregateByStoreAndVariantForSku(runId, skuId)
                : resultRepository.aggregateByStoreAndVariant(runId);

        Map<UUID, VariantMetrics[]> byStore = new LinkedHashMap<>();
        for (StoreVariantAggregate agg : aggregates) {
            UUID storeId = agg.getStoreId();
            VariantMetrics[] pair = byStore.computeIfAbsent(storeId, k -> new VariantMetrics[2]);
            VariantMetrics metrics = new VariantMetrics(
                    agg.getTotalUnits(), agg.getTotalRevenue(), agg.getTotalMargin());
            if (agg.isTestGroup()) {
                pair[1] = metrics;
            } else {
                pair[0] = metrics;
            }
        }

        List<SimulationBreakdownRowResponse> result = new ArrayList<>();
        for (Map.Entry<UUID, VariantMetrics[]> entry : byStore.entrySet()) {
            VariantMetrics control = entry.getValue()[0] != null ? entry.getValue()[0] : VariantMetrics.zero();
            VariantMetrics test = entry.getValue()[1] != null ? entry.getValue()[1] : VariantMetrics.zero();
            DeltaMetrics delta = calculateDelta(control, test);
            result.add(new SimulationBreakdownRowResponse(entry.getKey(), null, null, control, test, delta));
        }
        return result;
    }

    private List<SimulationBreakdownRowResponse> buildSkuBreakdown(UUID runId, UUID storeId) {
        List<SkuVariantAggregate> aggregates = (storeId != null)
                ? resultRepository.aggregateBySkuAndVariantForStore(runId, storeId)
                : resultRepository.aggregateBySkuAndVariant(runId);

        Map<UUID, VariantMetrics[]> bySku = new LinkedHashMap<>();
        for (SkuVariantAggregate agg : aggregates) {
            UUID skuIdLocal = agg.getSkuId();
            VariantMetrics[] pair = bySku.computeIfAbsent(skuIdLocal, k -> new VariantMetrics[2]);
            VariantMetrics metrics = new VariantMetrics(
                    agg.getTotalUnits(), agg.getTotalRevenue(), agg.getTotalMargin());
            if (agg.isTestGroup()) {
                pair[1] = metrics;
            } else {
                pair[0] = metrics;
            }
        }

        List<SimulationBreakdownRowResponse> result = new ArrayList<>();
        for (Map.Entry<UUID, VariantMetrics[]> entry : bySku.entrySet()) {
            VariantMetrics control = entry.getValue()[0] != null ? entry.getValue()[0] : VariantMetrics.zero();
            VariantMetrics test = entry.getValue()[1] != null ? entry.getValue()[1] : VariantMetrics.zero();
            DeltaMetrics delta = calculateDelta(control, test);
            result.add(new SimulationBreakdownRowResponse(null, entry.getKey(), null, control, test, delta));
        }
        return result;
    }

    private List<SimulationBreakdownRowResponse> buildDateBreakdown(UUID runId, UUID storeId, UUID skuId) {
        List<DateVariantAggregate> aggregates = fetchTimeseriesAggregates(runId, storeId, skuId);

        Map<LocalDate, VariantMetrics[]> byDate = new LinkedHashMap<>();
        for (DateVariantAggregate agg : aggregates) {
            LocalDate date = agg.getSimulationDate();
            VariantMetrics[] pair = byDate.computeIfAbsent(date, k -> new VariantMetrics[2]);
            VariantMetrics metrics = new VariantMetrics(
                    agg.getTotalUnits(), agg.getTotalRevenue(), agg.getTotalMargin());
            if (agg.isTestGroup()) {
                pair[1] = metrics;
            } else {
                pair[0] = metrics;
            }
        }

        List<SimulationBreakdownRowResponse> result = new ArrayList<>();
        for (Map.Entry<LocalDate, VariantMetrics[]> entry : byDate.entrySet()) {
            VariantMetrics control = entry.getValue()[0] != null ? entry.getValue()[0] : VariantMetrics.zero();
            VariantMetrics test = entry.getValue()[1] != null ? entry.getValue()[1] : VariantMetrics.zero();
            DeltaMetrics delta = calculateDelta(control, test);
            result.add(new SimulationBreakdownRowResponse(null, null, entry.getKey(), control, test, delta));
        }
        return result;
    }
}
