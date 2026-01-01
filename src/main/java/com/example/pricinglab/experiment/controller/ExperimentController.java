package com.example.pricinglab.experiment.controller;

import com.example.pricinglab.experiment.domain.Experiment;
import com.example.pricinglab.experiment.dto.ApprovalRequest;
import com.example.pricinglab.experiment.dto.CreateExperimentRequest;
import com.example.pricinglab.experiment.dto.ExperimentDetailResponse;
import com.example.pricinglab.experiment.dto.ExperimentMapper;
import com.example.pricinglab.experiment.dto.ExperimentResponse;
import com.example.pricinglab.experiment.dto.ModifyScopeRequest;
import com.example.pricinglab.experiment.dto.ScopeListResponse;
import com.example.pricinglab.experiment.service.ExperimentApprovalService;
import com.example.pricinglab.experiment.service.ExperimentScopeService;
import com.example.pricinglab.experiment.service.ExperimentService;
import com.example.pricinglab.simulation.service.SimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for experiment management.
 *
 * Provides endpoints for creating, viewing, and managing pricing experiments.
 *
 * Note: This is a SIMULATION tool only. No actual pricing changes are made.
 */
@RestController
@RequestMapping("/api/experiments")
@Tag(name = "Experiments", description = "Pricing experiment management APIs")
public class ExperimentController {

    private static final Logger log = LoggerFactory.getLogger(ExperimentController.class);

    private final ExperimentService experimentService;
    private final ExperimentApprovalService approvalService;
    private final ExperimentScopeService scopeService;
    private final SimulationService simulationService;
    private final ExperimentMapper mapper;

    public ExperimentController(
        ExperimentService experimentService,
        ExperimentApprovalService approvalService,
        ExperimentScopeService scopeService,
        SimulationService simulationService,
        ExperimentMapper mapper
    ) {
        this.experimentService = experimentService;
        this.approvalService = approvalService;
        this.scopeService = scopeService;
        this.simulationService = simulationService;
        this.mapper = mapper;
    }

    @PostMapping
    @Operation(summary = "Create a new experiment", description = "Creates a new pricing experiment in DRAFT status")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Experiment created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ResponseEntity<ExperimentResponse> createExperiment(
        @Valid @RequestBody CreateExperimentRequest request
    ) {
        log.info("Creating experiment: {}", request.name());
        Experiment experiment = experimentService.createExperiment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(experiment));
    }

    @GetMapping
    @Operation(summary = "List all experiments", description = "Returns a list of all experiments")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved experiments")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ResponseEntity<List<ExperimentResponse>> listExperiments() {
        List<Experiment> experiments = experimentService.listExperiments();
        List<ExperimentResponse> responses = experiments.stream()
            .map(mapper::toResponse)
            .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get experiment by ID", description = "Returns detailed information about a specific experiment")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved experiment"),
        @ApiResponse(responseCode = "404", description = "Experiment not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ResponseEntity<ExperimentDetailResponse> getExperiment(@PathVariable UUID id) {
        Experiment experiment = experimentService.getExperiment(id);
        return ResponseEntity.ok(mapper.toDetailResponse(experiment));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit experiment for approval",
        description = "Submits a DRAFT experiment for admin approval")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Experiment submitted for approval"),
        @ApiResponse(responseCode = "404", description = "Experiment not found"),
        @ApiResponse(responseCode = "409", description = "Invalid state transition")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ResponseEntity<ExperimentResponse> submitForApproval(@PathVariable UUID id) {
        log.info("Submitting experiment {} for approval", id);
        Experiment experiment = experimentService.submitForApproval(id);
        return ResponseEntity.ok(mapper.toResponse(experiment));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve or reject experiment",
        description = "Approves or rejects a pending experiment. Requires ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Experiment approved/rejected successfully"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "Experiment not found"),
        @ApiResponse(responseCode = "409", description = "Invalid state transition")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ExperimentResponse> processApproval(
        @PathVariable UUID id,
        @Valid @RequestBody ApprovalRequest request
    ) {
        log.info("Processing approval for experiment {}: approved={}", id, request.approved());
        Experiment experiment = approvalService.processApproval(id, request);
        return ResponseEntity.ok(mapper.toResponse(experiment));
    }

    @PostMapping("/{id}/run-simulation")
    @Operation(summary = "Run simulation for experiment",
        description = "Triggers a simulation run for an APPROVED experiment")
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Simulation started"),
        @ApiResponse(responseCode = "404", description = "Experiment not found"),
        @ApiResponse(responseCode = "409", description = "Experiment not in APPROVED status")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ResponseEntity<Void> runSimulation(@PathVariable UUID id) {
        log.info("Starting simulation for experiment {}", id);
        simulationService.startSimulation(id);
        return ResponseEntity.accepted().build();
    }

    // --- Scope Management Endpoints ---

    @PostMapping("/{id}/scope")
    @Operation(summary = "Add scope entries to experiment",
        description = "Adds store-SKU pairs to the experiment scope. Only allowed when experiment is in DRAFT status. " +
                      "Duplicates (both within request and against existing entries) are rejected with HTTP 400.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Scope entries added successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request: duplicate entries, invalid state, or validation error"),
        @ApiResponse(responseCode = "404", description = "Experiment, Store, or SKU not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ResponseEntity<ScopeListResponse> addScopeEntries(
        @PathVariable UUID id,
        @Valid @RequestBody ModifyScopeRequest request
    ) {
        log.info("Adding {} scope entries to experiment {}", request.entries().size(), id);
        ScopeListResponse response = scopeService.addScopeEntries(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/scope")
    @Operation(summary = "Remove scope entries from experiment",
        description = "Removes store-SKU pairs from the experiment scope. Only allowed when experiment is in DRAFT status. " +
                      "Entries that do not exist are silently ignored (bulk delete convenience).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Scope entries removed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid experiment state (not DRAFT)"),
        @ApiResponse(responseCode = "404", description = "Experiment not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ResponseEntity<ScopeListResponse> removeScopeEntries(
        @PathVariable UUID id,
        @Valid @RequestBody ModifyScopeRequest request
    ) {
        log.info("Removing {} scope entries from experiment {}", request.entries().size(), id);
        ScopeListResponse response = scopeService.removeScopeEntries(id, request);
        return ResponseEntity.ok(response);
    }
}
