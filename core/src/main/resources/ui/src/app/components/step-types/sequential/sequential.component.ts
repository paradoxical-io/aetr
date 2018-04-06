import {Component, Input, OnInit} from '@angular/core';
import {Step} from "../../../services/api.service";

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
