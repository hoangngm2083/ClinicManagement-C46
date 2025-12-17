package com.clinic.c46.MedicalPackageService.application.factory;

import com.clinic.c46.MedicalPackageService.application.strategy.BulkOpsStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BulkOpsStrategyFactory {

    private final List<BulkOpsStrategy> strategies;
    private Map<String, BulkOpsStrategy> strategyMap;

    private Map<String, BulkOpsStrategy> getStrategyMap() {
        if (strategyMap == null) {
            strategyMap = strategies.stream()
                    .collect(Collectors.toMap(
                            BulkOpsStrategy::getEntityType,
                            Function.identity()
                    ));
        }
        return strategyMap;
    }

    public BulkOpsStrategy getStrategy(String entityType) {
        BulkOpsStrategy strategy = getStrategyMap().get(entityType);
        if (strategy == null) {
            throw new IllegalArgumentException("No strategy found for entity type: " + entityType);
        }
        return strategy;
    }
}
