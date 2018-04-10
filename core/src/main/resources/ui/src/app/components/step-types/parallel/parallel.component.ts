import {Component, Input, OnInit} from '@angular/core';
import {Step} from "../../../model/model";

@Component({
    selector: 'app-parallel',
    templateUrl: './parallel.component.html',
    styleUrls: ['./parallel.component.css']
})
export class ParallelComponent implements OnInit {

    @Input() data: Step;

    constructor() {
    }

    ngOnInit() {
    }

}
