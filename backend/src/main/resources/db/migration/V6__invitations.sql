-- Slice 5b: an invitation is an application started by the organization (Q5g); the volunteer can decline it (Q5h).
-- For an invitation, applied_at is the moment of the invitation.

ALTER TABLE applications DROP CONSTRAINT ck_applications_status;
ALTER TABLE applications ADD CONSTRAINT ck_applications_status
    CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'WITHDRAWN', 'DECLINED'));

ALTER TABLE applications DROP CONSTRAINT ck_applications_origin;
ALTER TABLE applications ADD CONSTRAINT ck_applications_origin
    CHECK (origin IN ('APPLICATION', 'INVITATION'));
