import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { EditStepActionComponent } from './action.component';

describe('ActionComponent', () => {
  let component: EditStepActionComponent;
  let fixture: ComponentFixture<EditStepActionComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ EditStepActionComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(EditStepActionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
