import {AfterViewInit, Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {forkJoin} from "rxjs/observable/forkJoin";
import {ApiService} from "../../services/api.service";
import {ActivatedRoute} from "@angular/router";
import {RunState, RunTree} from "../../model/model";
import {ITreeOptions} from "angular-tree-component";

@Component({
    selector: 'app-run-details',
    templateUrl: './run-details.component.html',
    styleUrls: ['./run-details.component.css']
})
export class RunDetailsComponent implements OnInit, OnDestroy, AfterViewInit {

    constructor(private route: ActivatedRoute, private api: ApiService) {
    }

    private updateInterval: number;

    ngOnInit() {
        this.loadData();

        document.addEventListener('visibilitychange', () => {
            if (document.visibilityState === 'visible') {
                console.log("registering active tasks watcher");

                this.loadData();
            } else {
                this.unwatchTasks()
            }
        });
    }

    ngOnDestroy(): void {
        this.unwatchTasks();
    }

    unwatchTasks(): void {
        console.log("Clearing active tasks watcher");

        clearInterval(this.updateInterval);
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

    expand() {
        this.tree.treeModel.expandAll();
    }

    loadData() {
        let parent = this;

        this.route.paramMap.subscribe(x => {
            let rootId = x.get('id');

            function update() {
                let run = parent.api.getRun(rootId);

                forkJoin([run]).subscribe(results => {
                    parent.run = results[0];

                    if (parent.isComplete(parent.run.state)) {
                        parent.unwatchTasks();
                    }
                })
            }

            this.updateInterval = setInterval(update.bind(this), 5000);

            update.call(this);
        })
    }

    isComplete(state: RunState): boolean {
        return state == this.RunState.Error || state == this.RunState.Complete;
    }
}
