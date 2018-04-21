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
    mapper: Mapper;
    reducer: Reducer;
    action: Execution;
    children: Step[];
}

export class NashornReducer implements Reducer {
    type = ReducerType.js;

    js: string;
}

export interface Reducer {
    type: ReducerType;
}

export interface Mapper {
    type: MapperType;
}

export class NashornMapper implements Mapper {
    type = MapperType.js;

    js: string;
}

export interface Mapper {
    type: MapperType;
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

export enum ReducerType {
    js = "js",
    last = "last",
    none = 'no-op'
}

export enum MapperType {
    js = "js"
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
    executedAt: number;
    completedAt: number;
    stepTreeId: string;
}

export class GetRelatedRunsResult {
    runs: RunData[];
}

export class ListRunsData {
    id: string;
    root: string;
    state: RunState;
    stepTreeId: string;
    nodeName: string;
    result: string;
    lastUpdatedAt: number;
}

export class RunTree {
    id: string;
    root: string;
    state: RunState;
    result: string;
    input: string;
    stepTree: StepRoot;
    createdAt: number;
    executedAt: number;
    completedAt: number;
    children: RunTree[];
}