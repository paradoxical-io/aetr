import {Component, Input, OnInit} from '@angular/core';
import {Step} from "../../../../model/model";

@Component({
    selector: 'app-action',
    templateUrl: './action.component.html',
    styleUrls: ['./action.component.css']
})
export class EditStepActionComponent implements OnInit {

    @Input() step: Step;

    constructor() {
    }

    ngOnInit() {
    }

}