import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { EditStepParentComponent } from './parent.component';

describe('ParentComponent', () => {
  let component: EditStepParentComponent;
  let fixture: ComponentFixture<EditStepParentComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ EditStepParentComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(EditStepParentComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
