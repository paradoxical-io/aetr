// import { Component, OnInit } from '@angular/core';
// import {ApiService} from "../../services/api.service";
//
// @Component({
//   selector: 'app-list-runs',
//   templateUrl: './list-runs.component.html',
//   styleUrls: ['./list-runs.component.css']
// })
// export class ListRunsComponent implements OnInit {
//
//   constructor(private api: ApiService) { }
//
//   ngOnInit() {
//
//   }
// }

import {Component} from '@angular/core';

@Component({
    selector: 'embedded-sortable',
    template: `
        <h4>Move items between multi list sortable containers</h4>
        <div class="row">
            <div class="col-sm-3">
                Drag Containers <input type="checkbox" [(ngModel)]="dragOperation"/>
                <div >
                    <div class="col-sm3"
                         *ngFor="let container of containers; let i = index">
                        <div class="panel panel-warning"
                             dnd-sortable-container [sortableData]="container.widgets" [dropZones]="['widget-dropZone']">
                            <div class="panel-heading">
                                {{container.id}} - {{container.name}}
                            </div>
                            <div class="panel-body">
                                <ul class="list-group">
                                    <li *ngFor="let widget of container.widgets; let x = index" class="list-group-item"
                                        dnd-sortable [sortableIndex]="x" [dragEnabled]="!dragOperation"
                                        [dragData]="widget">{{widget.name}}</li>
                                </ul>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>`
})
export class ListRunsComponent {
    dragOperation: boolean = false;

    containers: Array<Container> = [
        new Container(1, 'Container 1', [new Widget('1'), new Widget('2')]),
        new Container(2, 'Container 2', [new Widget('3'), new Widget('4')]),
        new Container(3, 'Container 3', [new Widget('5'), new Widget('6')])
    ];

    widgets: Array<Widget> = [];
    addTo($event: any) {
        if ($event) {
            this.widgets.push($event.dragData);
        }
    }
}

class Container {
    constructor(public id: number, public name: string, public widgets: Array<Widget>) {}
}

class Widget {
    constructor(public name: string) {}
}