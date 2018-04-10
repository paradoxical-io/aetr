import {Component, OnInit, ViewChild} from '@angular/core';
import {forkJoin} from "rxjs/observable/forkJoin";
import {ApiService} from "../../services/api.service";
import {ActivatedRoute} from "@angular/router";
import {RunTree, Step, RunState} from "../../model/model";
import {ITreeOptions} from "angular-tree-component";

@Component({
    selector: 'app-run-details',
    templateUrl: './run-details.component.html',
    styleUrls: ['./run-details.component.css']
})
export class RunDetailsComponent implements OnInit {

    constructor(private route: ActivatedRoute, private api: ApiService) {
    }

    ngOnInit() {
        this.loadData();
    }

    run: RunTree;

    selectedRunChild: RunTree;

    RunState = RunState;

    @ViewChild('tree') tree;

    ngAfterViewInit() {
        this.tree.treeModel.expandAll();
    }

    selectNode(event) {
        this.selectedRunChild = event.node.data
    }

    getNodes(): RunTree[] {
        return [this.run]
    }

    getOptions(): ITreeOptions {
        return {
            idField: 'id',
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

    loadData() {
        this.route.paramMap.subscribe(x => {
            let rootId = x.get('id');

            let run = this.api.getRun(rootId);

            forkJoin([run]).subscribe(results => {
                this.run = results[0]
            })
        })
    }
}
