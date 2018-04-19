import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ReducerEditComponent } from './reducer-edit.component';

describe('ReducerEditComponent', () => {
  let component: ReducerEditComponent;
  let fixture: ComponentFixture<ReducerEditComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ReducerEditComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ReducerEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
