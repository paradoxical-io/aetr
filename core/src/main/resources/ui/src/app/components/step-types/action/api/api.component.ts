import {Component, Input, OnInit} from '@angular/core';
import {Step, ApiExecution} from "../../../../model/model";

@Component({
    selector: 'app-api',
    templateUrl: './api.component.html',
    styleUrls: ['./api.component.css']
})
export class ApiComponent implements OnInit {

    @Input() data: Step;

    action: ApiExecution;

    constructor() {
    }

    ngOnInit() {
        this.action = <ApiExecution>this.data.action
    }

}
