import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { RunDetailsItemComponent } from './run-details-item.component';

describe('RunDetailsItemComponent', () => {
  let component: RunDetailsItemComponent;
  let fixture: ComponentFixture<RunDetailsItemComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ RunDetailsItemComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(RunDetailsItemComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
