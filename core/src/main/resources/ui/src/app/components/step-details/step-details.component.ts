import {AfterViewInit, Component, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute} from "@angular/router";
import {ApiService} from "../../services/api.service";
import {forkJoin} from "rxjs/observable/forkJoin";
import {ITreeOptions} from "angular-tree-component";
import {RunData, Step, StepRoot, StepType} from "../../model/model";

@Component({
    selector: 'app-step-details',
    templateUrl: './step-details.component.html',
    styleUrls: ['./step-details.component.css'],
})
export class StepDetailsComponent implements OnInit, AfterViewInit {

    constructor(private route: ActivatedRoute, private api: ApiService) {
    }

    step: Step;

    StepType = StepType;

    selectedStep: Step = undefined;

    relatedParents: StepRoot[];

    relatedRuns: RunData[];

    @ViewChild('tree') tree;

    ngAfterViewInit() {
        this.tree.treeModel.expandAll();
    }

    getNodes(): Step[] {
        return [this.step]
    }

    selectNode(event) {
        this.selectedStep = event.node.data
    }

    unselectNode(event) {
        this.selectedStep = undefined;
    }

    expand() {
        this.tree.treeModel.expandAll();
    }

    getOptions(): ITreeOptions {
        return {
            idField: '_id',
            displayField: 'name',
            childrenField: 'children',
            allowDrag: (node) => {
                return false;
            },
            allowDrop: (node, {parent, index}) => {
                return false;
            }
        }
    }

    ngOnInit() {
        this.loadData()
    }

    loadData() {
        this.route.paramMap.subscribe(x => {
            let stepId = x.get('id');

            let getParents = this.api.getStepParents(stepId);
            let getRuns = this.api.listRelatedRuns(stepId);
            let getSTep = this.api.getStep(stepId);

            forkJoin([getParents, getRuns, getSTep]).subscribe(results => {
                this.relatedParents = results[0];
                this.relatedRuns = results[1];
                this.step = results[2];
                this.tree.treeModel.update();
            })
        })
    }
}