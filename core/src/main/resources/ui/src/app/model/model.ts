export class CreateStepRequest {
    name: string;
    stepType: StepType;
    action: Execution;
    children: string[];
}

export class CreateStepResponse {
    id: string
}

export class StepRoot {
    id: string;
    name: string;
    stepType: StepType;
}

export class Step {
    id: string;
    name: string;
    stepType: StepType;
    action: Execution;
    children: Step[];
}

export interface Execution {
    type: ExecutionType
}

export class ApiExecution implements Execution {
    type: ExecutionType = ExecutionType.api;
    url: string;
}

export class CreateRunResult {
    id: string
}

export enum ExecutionType {
    api = "api"
}

export enum StepType {
    Sequential = "Sequential",
    Parallel = "Parallel",
    Action = "Action"
}

export enum RunState {
    Pending = "Pending",
    Executing = "Executing",
    Error = "Error",
    Complete = "Complete"
}

export class RunData {
    state: RunState;
    id: string;
    result: string;
    createdAt: number;
    stepTreeId: string;
}

export class GetRelatedRunsResult {
    runs: RunData[];
}

export class RunTree {
    id: string;
    root: string;
    state: RunState;
    result: string;
    stepTree: StepRoot;
    children: RunTree[];
}