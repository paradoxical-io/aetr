import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { MapperEditComponent } from './mapper-edit.component';

describe('MapperEditComponent', () => {
  let component: MapperEditComponent;
  let fixture: ComponentFixture<MapperEditComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ MapperEditComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(MapperEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
