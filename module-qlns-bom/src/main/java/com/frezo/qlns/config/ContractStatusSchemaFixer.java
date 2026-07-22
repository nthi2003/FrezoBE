package com.frezo.qlns.config;

import com.frezo.qlns.common.StatusContarct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Repair schema drift: Hibernate historically mapped {@link StatusContarct} as ORDINAL
 * ({@code smallint} + check {@code status >= 0 AND status <= N}), while entity/API/demo SQL
 * now use VARCHAR + {@code EnumType.STRING}.
 * Runs before {@link ContractDataInitializer}.
 */
@Slf4j
@Component
@Order(70)
@RequiredArgsConstructor
public class ContractStatusSchemaFixer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        migrateStatusColumn("contract", "status");
        migrateStatusColumn("contract_history", "status_contarct");
    }

    private void migrateStatusColumn(String table, String column) {
        if (!tableExists(table) || !columnExists(table, column)) {
            return;
        }
        String udt = columnUdt(table, column);
        if (udt == null) {
            return;
        }
        if ("varchar".equals(udt) || "text".equals(udt) || "bpchar".equals(udt)) {
            return;
        }
        if (!"int2".equals(udt) && !"int4".equals(udt) && !"int8".equals(udt)) {
            log.warn("[contract-schema] {}.{} has unexpected type {} — skip migrate", table, column, udt);
            return;
        }

        dropNumericStatusChecks(table, column);

        String using = buildOrdinalUsingExpr(column);
        String sql = "ALTER TABLE " + table + " ALTER COLUMN " + column
                + " TYPE varchar(32) USING (" + using + ")";
        log.info("[contract-schema] migrating {}.{} from {} → varchar(32)", table, column, udt);
        jdbcTemplate.execute(sql);
        ensureVarcharStatusCheck(table, column);
        log.info("[contract-schema] migrated {}.{}", table, column);
    }

    /** Drop Hibernate ORDINAL check constraints like {@code status >= 0 AND status <= 15}. */
    private void dropNumericStatusChecks(String table, String column) {
        jdbcTemplate.query(
                "SELECT c.conname FROM pg_constraint c "
                        + "JOIN pg_class t ON t.oid = c.conrelid "
                        + "JOIN pg_namespace n ON n.oid = t.relnamespace "
                        + "WHERE n.nspname = 'public' AND t.relname = ? AND c.contype = 'c' "
                        + "AND pg_get_constraintdef(c.oid) LIKE ?",
                rs -> {
                    while (rs.next()) {
                        String name = rs.getString(1);
                        log.info("[contract-schema] dropping check constraint {}.{}", table, name);
                        jdbcTemplate.execute("ALTER TABLE " + table + " DROP CONSTRAINT IF EXISTS " + name);
                    }
                    return null;
                },
                table, "%" + column + "%");
    }

    private void ensureVarcharStatusCheck(String table, String column) {
        String constraint = table + "_" + column + "_varchar_check";
        String allowed = Arrays.stream(StatusContarct.values())
                .map(v -> "'" + v.name() + "'")
                .collect(Collectors.joining(", "));
        jdbcTemplate.execute("ALTER TABLE " + table + " DROP CONSTRAINT IF EXISTS " + constraint);
        jdbcTemplate.execute("ALTER TABLE " + table + " ADD CONSTRAINT " + constraint
                + " CHECK (" + column + " IS NULL OR " + column + " IN (" + allowed + "))");
    }

    private static String buildOrdinalUsingExpr(String column) {
        StringBuilder sb = new StringBuilder("CASE ").append(column);
        StatusContarct[] values = StatusContarct.values();
        for (int i = 0; i < values.length; i++) {
            sb.append(" WHEN ").append(i)
                    .append(" THEN '").append(values[i].name()).append("'");
        }
        sb.append(" ELSE 'DRAFT' END");
        return sb.toString();
    }

    private boolean tableExists(String table) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM information_schema.tables "
                        + "WHERE table_schema = 'public' AND table_name = ?)",
                Boolean.class, table);
        return Boolean.TRUE.equals(exists);
    }

    private boolean columnExists(String table, String column) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM information_schema.columns "
                        + "WHERE table_schema = 'public' AND table_name = ? AND column_name = ?)",
                Boolean.class, table, column);
        return Boolean.TRUE.equals(exists);
    }

    private String columnUdt(String table, String column) {
        return jdbcTemplate.query(
                "SELECT udt_name FROM information_schema.columns "
                        + "WHERE table_schema = 'public' AND table_name = ? AND column_name = ?",
                rs -> rs.next() ? rs.getString(1) : null,
                table, column);
    }
}
