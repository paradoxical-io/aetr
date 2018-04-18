package io.paradoxical.aetr.core.model;

public enum RunState {
    Pending(false),
    Executing(false),
    Error(true),
    Complete(true);

    private final boolean isCompleteState;

    RunState(final boolean isCompleteState) {

        this.isCompleteState = isCompleteState;
    }

    public boolean isTerminalState() {
        return isCompleteState;
    }
}
