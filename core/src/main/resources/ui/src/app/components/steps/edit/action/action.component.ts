import {Component, Input, OnInit} from '@angular/core';
import {Step} from "../../../../services/api.service";

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
