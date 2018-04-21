import {RunState} from "../model/model";
import {Injectable} from "@angular/core";

@Injectable()
export class Utils {
    RunState = RunState;

    isComplete(state: RunState): boolean {
        return state == this.RunState.Error || state == this.RunState.Complete;
    }
}