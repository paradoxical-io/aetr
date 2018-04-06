import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ParallelComponent } from './parallel.component';

describe('ParallelComponent', () => {
  let component: ParallelComponent;
  let fixture: ComponentFixture<ParallelComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ParallelComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ParallelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
