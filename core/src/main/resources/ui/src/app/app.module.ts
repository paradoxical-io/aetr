import {BrowserModule} from '@angular/platform-browser';
import {NgModule} from '@angular/core';

import {RouterModule, Routes} from "@angular/router";
import {AppComponent} from "./index/app.component";
import {ApiService} from "./services/api.service";
import {ListStepsComponent} from './components/list-steps/list-steps.component';
import {ListRunsComponent} from './components/list-runs/list-runs.component';
import {CreateStepComponent} from './components/create-step/create-step.component';
import {CreateRunComponent} from './components/create-run/create-run.component';
import {HttpClientModule} from "@angular/common/http";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {StepDetailsComponent} from './components/step-details/step-details.component';
import {TreeModule} from "angular-tree-component";
import {SequentialComponent} from './components/step-types/sequential/sequential.component';
import {ParallelComponent} from './components/step-types/parallel/parallel.component';
import {ActionComponent} from './components/step-types/action/action.component';
import {ApiComponent} from './components/step-types/action/api/api.component';
import {HeaderComponent} from './components/header/header.component';
import {OrderModule} from "ngx-order-pipe";
import {EditStepParentComponent} from './components/steps/edit/parent/parent.component';
import {DndModule} from "ng2-dnd";
import {RunDetailsComponent} from './components/run-details/run-details.component';
import {EditStepActionComponent} from "./components/steps/edit/action/action.component";
import { MapperEditComponent } from './components/mapper-edit/mapper-edit.component';
import { ReducerEditComponent } from './components/reducer-edit/reducer-edit.component';
import { NashornComponent } from './components/reducer-edit/nashorn/nashorn.component';
import { RunDetailsItemComponent } from './components/run-details/run-details-item/run-details-item.component';
import {MomentModule} from "angular2-moment";
import {Utils} from 'app/utils/state-utils';

const appRoutes: Routes = [
    {path: 'steps/new', component: CreateStepComponent},
    {path: 'steps/details/:id', component: StepDetailsComponent},
    {path: 'steps/edit/parent/:id', component: EditStepParentComponent},
    {path: 'steps/edit/action/:id', component: EditStepActionComponent},
    {path: 'runs/new/steps/:id', component: CreateRunComponent},
    {path: 'runs/details/:id', component: RunDetailsComponent},
    {path: 'runs/list', component: ListRunsComponent},
    {path: 'steps/list', component: ListStepsComponent},
    {
        path: '',
        redirectTo: '/steps/list',
        pathMatch: 'full'
    }
];

@NgModule({
    declarations: [
        AppComponent,
        ListStepsComponent,
        ListRunsComponent,
        CreateStepComponent,
        CreateRunComponent,
        StepDetailsComponent,
        SequentialComponent,
        ParallelComponent,
        ActionComponent,
        ApiComponent,
        HeaderComponent,
        EditStepParentComponent,
        EditStepActionComponent,
        RunDetailsComponent,
        MapperEditComponent,
        ReducerEditComponent,
        NashornComponent,
        RunDetailsItemComponent
    ],
    imports: [
        RouterModule.forRoot(appRoutes),
        DndModule.forRoot(),
        HttpClientModule,
        TreeModule,
        MomentModule,
        FormsModule,
        ReactiveFormsModule,
        BrowserModule,
        OrderModule
    ],
    providers: [ApiService, Utils],
    bootstrap: [AppComponent]
})
export class AppModule {
}
