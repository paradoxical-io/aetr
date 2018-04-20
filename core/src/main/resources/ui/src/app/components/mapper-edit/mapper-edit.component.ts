import {Component, Input, OnInit} from '@angular/core';
import {MapperType, NashornMapper, NashornReducer, Step} from "../../model/model";

@Component({
    selector: 'app-mapper-edit',
    templateUrl: './mapper-edit.component.html',
    styleUrls: ['./mapper-edit.component.css']
})
export class MapperEditComponent implements OnInit {
    @Input() step: Step;

    mappingPlaceHolder: string = "function apply(data) { \n\
    return data; \n\
}";

    js: string = this.mappingPlaceHolder;

    constructor() {
    }

    errorText: string = "";

    ngOnInit() {
        if (this.step.mapper && this.step.mapper.type == MapperType.js) {
            let existing = (<NashornMapper>this.step.mapper).js;

            if(existing.length > 0) {
                this.js = existing
            }
        }
    }

    setData() {
        this.errorText = "";

        if (this.js && this.js.length > 0) {
            if (this.js.indexOf("function apply(data)") == -1 || this.js.indexOf("return ") == -1) {
                this.errorText = "Mappers must have a javascript function of the signature:" +
                    " 'function apply(data){ ... }' with a valid return statement. " +
                    "Mapping function will not be applied"
            } else {
                this.step.mapper = <NashornMapper>{
                    type: MapperType.js,
                    js: this.js
                };
            }
        } else {
            this.step.mapper = undefined;
        }
    }
}
