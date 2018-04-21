import {Component, OnInit} from '@angular/core';
import {ApiService} from "../../../../services/api.service";
import {ActivatedRoute, Router} from "@angular/router";
import {Location} from '@angular/common';
import {Step, StepType} from "../../../../model/model";
import {forkJoin} from "rxjs/observable/forkJoin";

@Component({
    selector: 'app-parent',
    templateUrl: './parent.component.html',
    styleUrls: ['./parent.component.css']
})
export class EditStepParentComponent implements OnInit {

    constructor(private api: ApiService,
                private route: ActivatedRoute,
                private router: Router,
                private location: Location) {
    }

    step: Step;

    searchText: string = "";

    StepType = StepType;

    allSteps: Step[];

    // the array used in the UI
    mutatingStepList: Step[];

    errorText: string = "";

    ngOnInit() {
        this.route.paramMap.subscribe(x => {
            let stepId = x.get('id');

            let getStep = this.api.getStep(stepId);
            let getAllSteps = this.api.listSteps();

            forkJoin([getStep, getAllSteps]).subscribe(s => {
                this.step = s[0];

                this.allSteps = s[1];

                this.sortSteps();

                this.resyncSteps();
            })
        })
    }

    filterAllSteps() {
        this.applyFilter()
    }

    private applyFilter() {
        this.mutatingStepList = this.allSteps.filter(x => x.name.toLowerCase().indexOf(this.searchText.toLowerCase()) != -1)
    }

    private sortSteps() {
        this.allSteps.sort((a, b) => a.name.localeCompare(b.name))
    }

    resyncSteps() {
        this.mutatingStepList = this.allSteps.slice();

        if (this.searchText.length > 0) {
            this.applyFilter()
        }
    }

    save() {
        this.api.updateStep(this.step).subscribe(x => {
            this.router.navigateByUrl("/steps/list")
        }, err => {
            this.errorText = err.error.errors[0];
        })
    }

    clone() {
        this.api.insertStep({
            name: this.step.name + " copy",
            stepType: this.step.stepType,
            action: this.step.action,
            children: this.step.children.map(c => c.id)
        }).subscribe(x => {
            this.router.navigateByUrl("/steps/edit/parent/" + x.id)
        }, err => {
            this.errorText = err.error.errors[0];
        })
    }
}