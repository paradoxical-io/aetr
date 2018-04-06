import {Component, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute} from "@angular/router";
import {ApiService, Step, StepType} from "../../services/api.service";
import {ITreeOptions} from "angular-tree-component";

@Component({
    selector: 'app-step-details',
    templateUrl: './step-details.component.html',
    styleUrls: ['./step-details.component.css'],
})
export class StepDetailsComponent implements OnInit {

    constructor(private route: ActivatedRoute, private api: ApiService) {
    }

    step: Step;

    StepType = StepType;

    @ViewChild('tree') tree;

    ngAfterViewInit() {
        this.tree.treeModel.expandAll();
    }

    getNodes(): Step[] {
        return [this.step]
    }

    getOptions(): ITreeOptions {
        return {
            idField: '_id',
            displayField: 'name',
            childrenField: 'children',
            allowDrag: (node) => {
                return true;
            },
            allowDrop: (node, { parent, index }) => {
               return parent.data.stepType != this.StepType.Action;
            }
        }
    }

    ngOnInit() {
        this.route.paramMap.subscribe(x => {
            let stepId = x.get('id');

            this.api.getStep(stepId).subscribe(s => {
                this.step = s
            })
        })
    }

    raw(): string {
        return JSON.stringify(this.step);
    }
}