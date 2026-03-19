import { TestBed } from '@angular/core/testing';

import { ProfileStateServiceTs } from './profile-state.service.ts';

describe('ProfileStateServiceTs', () => {
  let service: ProfileStateServiceTs;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ProfileStateServiceTs);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
