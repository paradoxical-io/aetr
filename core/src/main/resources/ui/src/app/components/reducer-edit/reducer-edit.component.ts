import {Component, Input, OnInit} from '@angular/core';
import {Reducer, ReducerType, Step} from "../../model/model";

@Component({
    selector: 'app-reducer-edit',
    templateUrl: './reducer-edit.component.html',
    styleUrls: ['./reducer-edit.component.css']
})
export class ReducerEditComponent implements OnInit {

    @Input() step: Step;

    ReducerType = ReducerType;

    constructor() {
    }

    errorText: string = "";

    ngOnInit() {
        if(!this.step.reducer) {
            this.step.reducer = <Reducer> {
                type: ReducerType.none
            }
        }
    }
}
