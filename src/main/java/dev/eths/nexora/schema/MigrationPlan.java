package dev.eths.nexora.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MigrationPlan {
    private final List<MigrationStep> steps = new ArrayList<>();

    public void addStep(MigrationStep step) {
        steps.add(step);
    }

    public List<MigrationStep> getSteps() {
        return Collections.unmodifiableList(steps);
    }

    public boolean hasBlockedSteps() {
        return steps.stream().anyMatch(step -> step.getType() == MigrationStepType.BLOCKED || step.getType() == MigrationStepType.NEEDS_HINT);
    }
}
