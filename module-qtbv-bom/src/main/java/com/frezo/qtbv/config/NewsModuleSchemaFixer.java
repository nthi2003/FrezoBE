package com.frezo.qtbv.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Repair schema drift when Flyway is disabled locally ({@code ddl-auto=update} may miss
 * additive columns from {@code V202608061200__news_module_fields.sql}).
 */
@Slf4j
@Component
@Order(65)
@RequiredArgsConstructor
public class NewsModuleSchemaFixer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        if (!tableExists("articles")) {
            return;
        }
        ensureArticlesNewsColumns();
        ensureNewsCategoriesTable();
        ensureNewsMottosTable();
        ensureArticlePinsTable();
        ensureBannerPinColumn();
    }

    private void ensureArticlesNewsColumns() {
        addColumnIfMissing("articles", "category_id", "VARCHAR(50)");
        addColumnIfMissing("articles", "content_type", "VARCHAR(20)");
        addColumnIfMissing("articles", "external_url", "VARCHAR(1000)");
        if (addColumnIfMissing("articles", "display_on_news", "BOOLEAN")) {
            jdbcTemplate.execute("UPDATE articles SET display_on_news = true WHERE display_on_news IS NULL");
            jdbcTemplate.execute("ALTER TABLE articles ALTER COLUMN display_on_news SET DEFAULT true");
        }
        jdbcTemplate.execute("UPDATE articles SET content_type = 'ARTICLE' WHERE content_type IS NULL");
    }

    private void ensureBannerPinColumn() {
        if (!tableExists("banners")) {
            return;
        }
        if (addColumnIfMissing("banners", "pin_for_news_page", "BOOLEAN")) {
            jdbcTemplate.execute("UPDATE banners SET pin_for_news_page = false WHERE pin_for_news_page IS NULL");
            jdbcTemplate.execute("ALTER TABLE banners ALTER COLUMN pin_for_news_page SET DEFAULT false");
        }
    }

    private void ensureNewsCategoriesTable() {
        if (tableExists("news_categories")) {
            return;
        }
        log.info("[news-schema] creating news_categories");
        jdbcTemplate.execute("""
                CREATE TABLE news_categories (
                    id              VARCHAR(36)  NOT NULL PRIMARY KEY,
                    name            VARCHAR(100) NOT NULL,
                    color           VARCHAR(20)  NOT NULL DEFAULT '#16a34a',
                    organization_id VARCHAR(50),
                    order_index     INTEGER      DEFAULT 0,
                    created_by      VARCHAR(50),
                    created_date    TIMESTAMP,
                    updated_by      VARCHAR(50),
                    updated_date    TIMESTAMP,
                    is_deleted      BOOLEAN      DEFAULT false,
                    deleted_at      TIMESTAMP,
                    deleted_by      VARCHAR(50)
                )
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_news_categories_org
                    ON news_categories (organization_id, order_index)
                    WHERE is_deleted = false OR is_deleted IS NULL
                """);
    }

    private void ensureNewsMottosTable() {
        if (tableExists("news_mottos")) {
            return;
        }
        log.info("[news-schema] creating news_mottos");
        jdbcTemplate.execute("""
                CREATE TABLE news_mottos (
                    id              VARCHAR(36)  NOT NULL PRIMARY KEY,
                    content         TEXT         NOT NULL,
                    author          VARCHAR(200),
                    created_by      VARCHAR(50),
                    created_date    TIMESTAMP,
                    updated_by      VARCHAR(50),
                    updated_date    TIMESTAMP,
                    is_deleted      BOOLEAN      DEFAULT false,
                    deleted_at      TIMESTAMP,
                    deleted_by      VARCHAR(50)
                )
                """);
    }

    private void ensureArticlePinsTable() {
        if (tableExists("article_pins")) {
            return;
        }
        log.info("[news-schema] creating article_pins");
        jdbcTemplate.execute("""
                CREATE TABLE article_pins (
                    id              VARCHAR(36)  NOT NULL PRIMARY KEY,
                    article_id      VARCHAR(50)  NOT NULL,
                    organization_id VARCHAR(50)  NOT NULL,
                    sort_order      INTEGER      NOT NULL DEFAULT 0,
                    created_by      VARCHAR(50),
                    created_date    TIMESTAMP,
                    updated_by      VARCHAR(50),
                    updated_date    TIMESTAMP,
                    is_deleted      BOOLEAN      DEFAULT false,
                    deleted_at      TIMESTAMP,
                    deleted_by      VARCHAR(50),
                    CONSTRAINT uk_article_pin_org_article UNIQUE (organization_id, article_id)
                )
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_article_pins_org
                    ON article_pins (organization_id, sort_order)
                    WHERE is_deleted = false OR is_deleted IS NULL
                """);
    }

    private boolean addColumnIfMissing(String table, String column, String ddlType) {
        if (columnExists(table, column)) {
            return false;
        }
        log.info("[news-schema] adding {}.{}", table, column);
        jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + ddlType);
        return true;
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
}
