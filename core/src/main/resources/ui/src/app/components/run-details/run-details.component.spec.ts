import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { RunDetailsComponent } from './run-details.component';

describe('RunDetailsComponent', () => {
  let component: RunDetailsComponent;
  let fixture: ComponentFixture<RunDetailsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ RunDetailsComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(RunDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
