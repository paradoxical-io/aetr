import {Component, OnInit} from '@angular/core';
import {ApiService, Step} from "../../services/api.service";

@Component({
    selector: 'app-list-steps',
    templateUrl: './list-steps.component.html',
    styleUrls: ['./list-steps.component.css']
})
export class ListStepsComponent implements OnInit {

    constructor(private api: ApiService) {
    }

    steps: Step[];

    ngOnInit() {
        this.api.listSteps().subscribe(s => this.steps = s)
    }
}
