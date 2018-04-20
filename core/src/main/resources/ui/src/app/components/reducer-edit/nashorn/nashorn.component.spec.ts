import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { NashornComponent } from './nashorn.component';

describe('NashornComponent', () => {
  let component: NashornComponent;
  let fixture: ComponentFixture<NashornComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ NashornComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(NashornComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
