import {Injectable} from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs/Observable";
import 'rxjs/add/operator/map'
import {CreateRunResult, CreateStepRequest, CreateStepResponse, GetRelatedRunsResult, RunData, RunTree, Step, StepRoot} from "../model/model";

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

    createRun(stepId: string, data: string): Observable<string> {
        return this.http.post<CreateRunResult>(`api/v1/runs/step/${stepId}`, {
            input: data
        }).map(x => x.id)
    }

    getRun(rootId: string): Observable<RunTree> {
        return this.http.get<RunTree>(`/api/v1/runs/${rootId}`)
    }

    listRelatedRuns(stepId: string): Observable<RunData[]> {
        return this.http.get<GetRelatedRunsResult>(`/api/v1/runs/related/step/${stepId}`).map(x => x.runs)
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

