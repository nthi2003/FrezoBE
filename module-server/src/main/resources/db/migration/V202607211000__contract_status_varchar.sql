-- Manual / future Flyway: contract.status was historically smallint (Hibernate EnumType.ORDINAL)
-- plus check constraint (status >= 0 AND status <= 15). Runtime repair: ContractStatusSchemaFixer.
-- When Flyway is enabled, prefer this script (idempotent) over the Java fixer.

DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'contract' AND column_name = 'status'
      AND udt_name IN ('int2', 'int4', 'int8')
  ) THEN
    ALTER TABLE contract DROP CONSTRAINT IF EXISTS contract_status_check;
    ALTER TABLE contract ALTER COLUMN status TYPE varchar(32) USING (
      CASE status
        WHEN 0 THEN 'DRAFT'
        WHEN 1 THEN 'PENDING_APPROVAL'
        WHEN 2 THEN 'NEGOTIATING'
        WHEN 3 THEN 'NO_YEP_EFFECTIVE'
        WHEN 4 THEN 'ACTIVE'
        WHEN 5 THEN 'SUSPENDED'
        WHEN 6 THEN 'WAITING_FOR_OP'
        WHEN 7 THEN 'OP_PROCESSING'
        WHEN 8 THEN 'WAITING_FOR_RV'
        WHEN 9 THEN 'RV_REVIEWING'
        WHEN 10 THEN 'OP_DONE'
        WHEN 11 THEN 'RV_DONE'
        WHEN 12 THEN 'RV_REJECTED'
        WHEN 13 THEN 'OP_REWORK'
        WHEN 14 THEN 'COMPLETED'
        WHEN 15 THEN 'CANCELLED'
        ELSE 'DRAFT'
      END
    );
    ALTER TABLE contract DROP CONSTRAINT IF EXISTS contract_status_varchar_check;
    ALTER TABLE contract ADD CONSTRAINT contract_status_varchar_check
      CHECK (status IS NULL OR status IN (
        'DRAFT','PENDING_APPROVAL','NEGOTIATING','NO_YEP_EFFECTIVE','ACTIVE','SUSPENDED',
        'WAITING_FOR_OP','OP_PROCESSING','WAITING_FOR_RV','RV_REVIEWING','OP_DONE','RV_DONE',
        'RV_REJECTED','OP_REWORK','COMPLETED','CANCELLED'
      ));
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'contract_history'
      AND column_name = 'status_contarct'
      AND udt_name IN ('int2', 'int4', 'int8')
  ) THEN
    ALTER TABLE contract_history DROP CONSTRAINT IF EXISTS contract_history_status_contarct_check;
    ALTER TABLE contract_history ALTER COLUMN status_contarct TYPE varchar(32) USING (
      CASE status_contarct
        WHEN 0 THEN 'DRAFT'
        WHEN 1 THEN 'PENDING_APPROVAL'
        WHEN 2 THEN 'NEGOTIATING'
        WHEN 3 THEN 'NO_YEP_EFFECTIVE'
        WHEN 4 THEN 'ACTIVE'
        WHEN 5 THEN 'SUSPENDED'
        WHEN 6 THEN 'WAITING_FOR_OP'
        WHEN 7 THEN 'OP_PROCESSING'
        WHEN 8 THEN 'WAITING_FOR_RV'
        WHEN 9 THEN 'RV_REVIEWING'
        WHEN 10 THEN 'OP_DONE'
        WHEN 11 THEN 'RV_DONE'
        WHEN 12 THEN 'RV_REJECTED'
        WHEN 13 THEN 'OP_REWORK'
        WHEN 14 THEN 'COMPLETED'
        WHEN 15 THEN 'CANCELLED'
        ELSE 'DRAFT'
      END
    );
    ALTER TABLE contract_history DROP CONSTRAINT IF EXISTS contract_history_status_contarct_varchar_check;
    ALTER TABLE contract_history ADD CONSTRAINT contract_history_status_contarct_varchar_check
      CHECK (status_contarct IS NULL OR status_contarct IN (
        'DRAFT','PENDING_APPROVAL','NEGOTIATING','NO_YEP_EFFECTIVE','ACTIVE','SUSPENDED',
        'WAITING_FOR_OP','OP_PROCESSING','WAITING_FOR_RV','RV_REVIEWING','OP_DONE','RV_DONE',
        'RV_REJECTED','OP_REWORK','COMPLETED','CANCELLED'
      ));
  END IF;
END $$;
