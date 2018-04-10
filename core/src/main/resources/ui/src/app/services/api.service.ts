import {Injectable} from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs/Observable";
import 'rxjs/add/operator/map'

@Injectable()
export class ApiService {

    constructor(private http: HttpClient) {
    }

    listSteps(): Observable<Step[]> {
        return this.http.get<Step[]>("/api/v1/steps")
    }

    getStep(id: string): Observable<Step> {
        return this.http.get<Step>(`/api/v1/steps/${id}`)
    }

    insertStep(request: CreateStepRequest): Observable<CreateStepResponse> {
        return this.http.post<CreateStepResponse>(`api/v1/steps`, request)
    }

    deleteStep(id: string): Observable<void> {
        return this.http.delete(`api/v1/steps/${id}`).map(x => null)
    }

    getStepParents(id: string): Observable<StepRoot[]> {
        return this.http.get<StepRoot[]>(`api/v1/steps/${id}/parents`)
    }

    updateStep(step: Step): Observable<void> {
        return this.http.put<Step>(`api/v1/steps/slim`, {
            id: step.id,
            name: step.name,
            stepType: step.stepType,
            action: step.action,
            children: step.children.map(c => c.id)
        }).map(x => null)
    }

    setChildren(id: string, children: string[]): Observable<void> {
        return this.http.post<string[]>(`api/v1/steps/${id}/children`, {
            children: children
        }).map(x => null)
    }
}

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

export enum ExecutionType {
    api = "api"
}

export enum StepType {
    Sequential = "Sequential",
    Parallel = "Parallel",
    Action = "Action"
}