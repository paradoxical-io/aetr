import {Component, OnInit} from '@angular/core';
import {ApiService, Step, StepRoot} from "../../services/api.service";

@Component({
    selector: 'app-list-steps',
    templateUrl: './list-steps.component.html',
    styleUrls: ['./list-steps.component.css']
})
export class ListStepsComponent implements OnInit {

    constructor(private api: ApiService) {
    }

    steps: Step[];

    selectedId: string;

    errorText: string = "";

    selectedRelatedParents: StepRoot[] = []

    ngOnInit() {
        this.loadData()
    }

    private loadData(): void {
        this.api.listSteps().subscribe(s => this.steps = s)
    }

    selectToggle(id: string) {
        if(this.selectedId == id) {
            this.selectedId = undefined;
        } else {
            this.selectedId = id
        }
    }

    deleteSelected(): void {
        this.api.deleteStep(this.selectedId).subscribe(x => {
            this.errorText = "";

            this.loadData()
        }, err => {
            this.api.getStepParents(this.selectedId).subscribe(parents => {
                this.errorText = "Could not delete step.  It may be related to other steps and needs to be unassociated to it";

                this.selectedRelatedParents = parents;
            })
        })
    }
}
