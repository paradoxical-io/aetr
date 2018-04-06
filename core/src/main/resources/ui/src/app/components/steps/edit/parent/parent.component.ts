import {Component, Input, OnInit} from '@angular/core';
import {ApiService, Step, StepType} from "../../../../services/api.service";
import {ActivatedRoute, Router} from "@angular/router";

@Component({
  selector: 'app-parent',
  templateUrl: './parent.component.html',
  styleUrls: ['./parent.component.css']
})
export class EditStepParentComponent implements OnInit {

  constructor(private api: ApiService, private route: ActivatedRoute, private router: Router) { }

  step: Step;

  StepType = StepType;

  allSteps: Step[];

  ngOnInit() {
      this.route.paramMap.subscribe(x => {
          let stepId = x.get('id');

          this.api.getStep(stepId).subscribe(s => {
              this.step = s;

              this.api.listSteps().subscribe(st => {
                  this.allSteps = st;
              })
          })
      })
  }

  save() {
      this.api.updateStep(this.step).subscribe(x => {
          this.router.navigateByUrl("/steps/details/" + this.step.id)
      })
  }
}