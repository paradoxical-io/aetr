import {Component, Input, OnInit} from '@angular/core';
import {Step} from "../../../../model/model";
import {ApiService} from "../../../../services/api.service";
import {ActivatedRoute, Router} from "@angular/router";
import {forkJoin} from "rxjs/observable/forkJoin";

@Component({
    selector: 'app-edit-action',
    templateUrl: './action.component.html',
    styleUrls: ['./action.component.css']
})
export class EditStepActionComponent implements OnInit {

    @Input() step: Step;

    constructor(private api: ApiService,
                private route: ActivatedRoute,
                private router: Router) {
    }

    ngOnInit() {
        this.route.paramMap.subscribe(x => {
            let stepId = x.get('id');

            let getStep = this.api.getStep(stepId);

            forkJoin([getStep]).subscribe(s => {
                this.step = s[0];
            })
        })
    }

    save() {
        this.api.updateStep(this.step).subscribe(x => {
            this.router.navigateByUrl("/steps/list")
        })
    }
}