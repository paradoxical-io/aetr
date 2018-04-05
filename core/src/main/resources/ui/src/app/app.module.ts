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
import {FormsModule} from "@angular/forms";

const appRoutes: Routes = [
    {path: 'steps/new', component: CreateStepComponent},
    {path: 'runs/new', component: CreateRunComponent},
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
        CreateRunComponent
    ],
    imports: [
        RouterModule.forRoot(appRoutes),
        HttpClientModule,
        FormsModule,
        BrowserModule
    ],
    providers: [ApiService],
    bootstrap: [AppComponent]
})
export class AppModule {
}
