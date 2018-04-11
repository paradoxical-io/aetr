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

const appRoutes: Routes = [
    {path: 'steps/new', component: CreateStepComponent},
    {path: 'steps/details/:id', component: StepDetailsComponent},
    {path: 'steps/edit/parent/:id', component: EditStepParentComponent},
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
        RunDetailsComponent
    ],
    imports: [
        RouterModule.forRoot(appRoutes),
        DndModule.forRoot(),
        HttpClientModule,
        TreeModule,
        FormsModule,
        ReactiveFormsModule,
        BrowserModule,
        OrderModule
    ],
    providers: [ApiService],
    bootstrap: [AppComponent]
})
export class AppModule {
}
