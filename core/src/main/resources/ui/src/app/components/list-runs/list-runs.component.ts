import { Component, OnInit } from '@angular/core';
import {ApiService} from "../../services/api.service";

@Component({
  selector: 'app-list-runs',
  templateUrl: './list-runs.component.html',
  styleUrls: ['./list-runs.component.css']
})
export class ListRunsComponent implements OnInit {

  constructor(private api: ApiService) { }

  ngOnInit() {

  }
}
