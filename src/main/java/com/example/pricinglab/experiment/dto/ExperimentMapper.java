package com.example.pricinglab.experiment.dto;

import com.example.pricinglab.experiment.domain.Experiment;
import com.example.pricinglab.experiment.domain.ExperimentGuardrails;
import com.example.pricinglab.experiment.domain.ExperimentLever;
import com.example.pricinglab.experiment.domain.ExperimentScope;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper for converting between Experiment entities and DTOs.
 */
@Component
public class ExperimentMapper {

    public ExperimentResponse toResponse(Experiment experiment) {
        return new ExperimentResponse(
            experiment.getId(),
            experiment.getName(),
            experiment.getDescription(),
            experiment.getStatus(),
            experiment.getStartDate(),
            experiment.getEndDate(),
            experiment.getBusinessJustification(),
            experiment.getHypothesis(),
            experiment.getCreatedBy(),
            experiment.getCreatedAt(),
            experiment.getApprovedBy(),
            experiment.getRejectionReason(),
            experiment.getScopes().size(),
            experiment.getLevers().size(),
            experiment.getGuardrails() != null
        );
    }

    public ExperimentDetailResponse toDetailResponse(Experiment experiment) {
        List<ExperimentDetailResponse.ScopeDto> scopeDtos = experiment.getScopes().stream()
            .map(this::toScopeDto)
            .toList();

        List<ExperimentDetailResponse.LeverDto> leverDtos = experiment.getLevers().stream()
            .map(this::toLeverDto)
            .toList();

        ExperimentDetailResponse.GuardrailsDto guardrailsDto = experiment.getGuardrails() != null
            ? toGuardrailsDto(experiment.getGuardrails())
            : null;

        return new ExperimentDetailResponse(
            experiment.getId(),
            experiment.getName(),
            experiment.getDescription(),
            experiment.getStatus(),
            experiment.getStartDate(),
            experiment.getEndDate(),
            experiment.getBusinessJustification(),
            experiment.getHypothesis(),
            experiment.getCreatedBy(),
            experiment.getCreatedAt(),
            experiment.getUpdatedBy(),
            experiment.getUpdatedAt(),
            experiment.getApprovedBy(),
            experiment.getRejectionReason(),
            scopeDtos,
            leverDtos,
            guardrailsDto
        );
    }

    private ExperimentDetailResponse.ScopeDto toScopeDto(ExperimentScope scope) {
        return new ExperimentDetailResponse.ScopeDto(
            scope.getId(),
            scope.getStore().getId(),
            scope.getStore().getStoreCode(),
            scope.getStore().getStoreName(),
            scope.getSku().getId(),
            scope.getSku().getSkuCode(),
            scope.getSku().getSkuName(),
            scope.isTestGroup()
        );
    }

    private ExperimentDetailResponse.LeverDto toLeverDto(ExperimentLever lever) {
        return new ExperimentDetailResponse.LeverDto(
            lever.getId(),
            lever.getSku().getId(),
            lever.getSku().getSkuCode(),
            lever.getSku().getSkuName(),
            lever.getLeverType().name(),
            lever.getLeverValue(),
            lever.getDescription()
        );
    }

    private ExperimentDetailResponse.GuardrailsDto toGuardrailsDto(ExperimentGuardrails guardrails) {
        return new ExperimentDetailResponse.GuardrailsDto(
            guardrails.getMaxDiscountPercentage(),
            guardrails.getMaxMarkupPercentage(),
            guardrails.getMinMarginPercentage(),
            guardrails.getMaxRevenueImpactPercentage(),
            guardrails.isPreventBelowCost(),
            guardrails.isEnforcePricePoints()
        );
    }
}
