import {Component, OnInit} from '@angular/core';
import {ApiService, CreateStepRequest, Execution, ExecutionType, StepType} from "../../services/api.service";
import {Router} from "@angular/router";

@Component({
    selector: 'app-create-step',
    templateUrl: './create-step.component.html',
    styleUrls: ['./create-step.component.css']
})
export class CreateStepComponent implements OnInit {

    constructor(private api: ApiService, private router: Router) {
    }

    StepType = StepType;

    action = <Execution>{
        type: ExecutionType.api
    };

    stepType;
    stepName;

    ngOnInit() {
    }

    submit() {
        // TODO, properly send in the right action...
        let cleanedAction = this.action;

        if (this.stepType != StepType.Action) {
            cleanedAction = undefined
        }

        let request: CreateStepRequest = {
            name: this.stepName,
            action: cleanedAction,
            stepType: this.stepType,
            children: []
        };

        this.api.insertStep(request).subscribe(_ => {
            this.router.navigateByUrl("/steps/list")
        })
    }
}
