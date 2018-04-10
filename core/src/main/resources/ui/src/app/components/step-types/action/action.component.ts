import {Component, Input, OnInit} from '@angular/core';
import {Step} from "../../../model/model";

@Component({
  selector: 'app-action',
  templateUrl: './action.component.html',
  styleUrls: ['./action.component.css']
})
export class ActionComponent implements OnInit {

  constructor() { }

  @Input() data: Step;

  ngOnInit() {
  }

}
