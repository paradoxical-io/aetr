import {Component, Input, OnInit} from '@angular/core';
import {NashornReducer, ReducerType, Step} from "../../../model/model";

@Component({
  selector: 'app-nashorn',
  templateUrl: './nashorn.component.html',
  styleUrls: ['./nashorn.component.css']
})
export class NashornComponent implements OnInit {

    @Input() step: Step;

    reducerPlaceHolder: string = "function apply(data) { \n\
    return data; \n\
}";

    js: string = this.reducerPlaceHolder;

    constructor() {
    }

    errorText: string = "";

    ngOnInit() {
        if (this.step.reducer && this.step.reducer.type == ReducerType.js) {
            let existing = (<NashornReducer>this.step.reducer).js;

            if(existing.length > 0) {
                this.js = existing
            }
        }
    }

    setData() {
        this.errorText = "";

        if (this.js && this.js.length > 0) {
            if (this.js.indexOf("function apply(data)") == -1 || this.js.indexOf("return ") == -1) {
                this.errorText = "Reducers must have a javascript function of the signature:" +
                    " 'function apply(data){ ... }' with a valid return statement. " +
                    "Mapping function will not be applied"
            } else {
                this.step.reducer = <NashornReducer>{
                    type: ReducerType.js,
                    js: this.js
                };
            }
        } else {
            this.step.mapper = undefined;
        }
    }

}
