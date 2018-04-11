import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { StepDetailsComponent } from './step-details.component';

describe('StepDetailsComponent', () => {
  let component: StepDetailsComponent;
  let fixture: ComponentFixture<StepDetailsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ StepDetailsComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(StepDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
