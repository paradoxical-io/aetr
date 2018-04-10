import {Component, OnInit} from '@angular/core';
import {ApiService} from "../../services/api.service";
import {ActivatedRoute} from "@angular/router";
import {Step} from "../../model/model";

@Component({
    selector: 'app-create-run',
    templateUrl: './create-run.component.html',
    styleUrls: ['./create-run.component.css']
})
export class CreateRunComponent implements OnInit {

    constructor(private route: ActivatedRoute, private api: ApiService) {
    }

    step: Step;

    ngOnInit() {
        this.route.paramMap.subscribe(x => {
            let stepId = x.get('id');

            this.api.getStep(stepId).subscribe(s => {
                this.step = s;
            })
        })
    }
}
