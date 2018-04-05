import {Injectable} from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs/Observable";
import 'rxjs/add/operator/map'

@Injectable()
export class ApiService {

    constructor(private http: HttpClient) {
    }

    listSteps(): Observable<StepRoot[]> {
        return this.http.get<StepRoot[]>("/api/v1/steps")
    }

    getStepTree(id: string): Observable<Step> {
        return this.http.get<Step>(`/api/v1/steps/${id}`)
    }

    upsertStep(step: Step): Observable<void> {
        return this.http.put<Step>(`api/v1/steps`, step).map(x => null)
    }

    setChildren(id: string, children: string[]): Observable<void> {
        return this.http.post<string[]>(`api/v1/steps/${id}/children`, {
            "children": children
        }).map(x => null)
    }
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
    root: Step;
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
    api
}

export enum StepType {
    Sequential,
    Parallel,
    Action
}