import {Component, Input, OnInit} from '@angular/core';
import {MapperType, NashornMapper, Step} from "../../model/model";

@Component({
    selector: 'app-mapper-edit',
    templateUrl: './mapper-edit.component.html',
    styleUrls: ['./mapper-edit.component.css']
})
export class MapperEditComponent implements OnInit {
    @Input() step: Step;

    js: string;

    mappingPlaceHolder: string = "function apply(data) { \n\
    return ... \n\
}";

    constructor() {
    }

    errorText: string = "";

    ngOnInit() {
        if (this.step.mapper && this.step.mapper.type == MapperType.js) {
            this.js = (<NashornMapper>this.step.mapper).js
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
