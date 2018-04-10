import {Component, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute} from "@angular/router";
import {ApiService} from "../../services/api.service";
import {ITreeOptions} from "angular-tree-component";
import {Step, StepRoot, StepType} from "../../model/model";

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

    selectedStep: Step = undefined;

    relatedParents: StepRoot[];

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

            this.api.getStepParents(stepId).subscribe(parents => {
                this.api.getStep(stepId).subscribe(s => {
                    this.step = s;
                    this.relatedParents = parents;
                    this.tree.treeModel.update();
                })
            })
        })
    }
}