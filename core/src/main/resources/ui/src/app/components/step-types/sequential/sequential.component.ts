import {Component, Input, OnInit} from '@angular/core';
import {Step} from "../../../model/model";

@Component({
    selector: 'app-sequential',
    templateUrl: './sequential.component.html',
    styleUrls: ['./sequential.component.css']
})
export class SequentialComponent implements OnInit {

    @Input() data: Step;

    constructor() {
    }

    ngOnInit() {
    }

}
