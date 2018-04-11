import { Component, OnInit } from '@angular/core';
import {ApiService} from "../../services/api.service";
import {ListRunsData, RunState} from "../../model/model";

@Component({
  selector: 'app-list-runs',
  templateUrl: './list-runs.component.html',
  styleUrls: ['./list-runs.component.css']
})
export class ListRunsComponent implements OnInit {

  constructor(private api: ApiService) { }

  RunState = RunState;

  runs: ListRunsData[];

  runStates = [RunState.Pending, RunState.Executing, RunState.Complete, RunState.Error];

  runStateToView = RunState.Pending;

  ngOnInit() {
    this.selectState(this.runStateToView)
  }

  selectState(state: RunState) {
      this.api.listRuns(state).subscribe(x => this.runs = x)
  }
}
