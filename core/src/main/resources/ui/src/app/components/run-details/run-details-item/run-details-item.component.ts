import {Component, Input, OnInit} from '@angular/core';
import {RunState, RunTree} from "../../../model/model";
import {Utils} from "../../../utils/state-utils";

@Component({
    selector: 'app-run-details-item',
    templateUrl: './run-details-item.component.html',
    styleUrls: ['./run-details-item.component.css']
})
export class RunDetailsItemComponent implements OnInit {

    @Input() run: RunTree;

    RunState = RunState;

    constructor(public utils: Utils) {
    }

    ngOnInit() {
    }

    timeInPending() {
        return this.run.executedAt - this.run.createdAt
    }

    timeInExecuting() {
        return this.run.completedAt - this.run.executedAt
    }
}
