import {Component, OnInit} from '@angular/core';
import {forkJoin} from "rxjs/observable/forkJoin";
import {ApiService} from "../../services/api.service";
import {ActivatedRoute} from "@angular/router";
import {RunTree} from "../../model/model";

@Component({
    selector: 'app-run-details',
    templateUrl: './run-details.component.html',
    styleUrls: ['./run-details.component.css']
})
export class RunDetailsComponent implements OnInit {

    constructor(private route: ActivatedRoute, private api: ApiService) {
    }

    ngOnInit() {
        this.loadData();
    }

    run: RunTree;

    loadData() {
        this.route.paramMap.subscribe(x => {
            let rootId = x.get('id');

            let run = this.api.getRun(rootId);

            forkJoin([run]).subscribe(results => {
                this.run = results[0]
            })
        })
    }
}
