import {Component, OnInit} from '@angular/core';
import {ApiService} from "../../services/api.service";
import {ActivatedRoute, Router} from "@angular/router";
import {Step} from "../../model/model";

@Component({
    selector: 'app-create-run',
    templateUrl: './create-run.component.html',
    styleUrls: ['./create-run.component.css']
})
export class CreateRunComponent implements OnInit {

    constructor(
        private route: ActivatedRoute,
        private api: ApiService,
        private router: Router
        ) {
    }

    step: Step;

    input: string;

    ngOnInit() {
        this.route.paramMap.subscribe(x => {
            let stepId = x.get('id');

            this.api.getStep(stepId).subscribe(s => {
                this.step = s;
            })
        })
    }

    create() {
        this.api.createRun(this.step.id, this.input).subscribe(runId => {
            this.router.navigateByUrl("/runs/details/" + runId)
        })
    }
}
