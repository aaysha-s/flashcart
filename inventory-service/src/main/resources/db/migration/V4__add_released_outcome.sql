ALTER TABLE order_reservations DROP CONSTRAINT order_reservations_outcome_check;
ALTER TABLE order_reservations ADD CONSTRAINT order_reservations_outcome_check
    CHECK (outcome IN ('RESERVED', 'REJECTED', 'RELEASED'));