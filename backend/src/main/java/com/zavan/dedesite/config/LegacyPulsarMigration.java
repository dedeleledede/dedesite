package com.zavan.dedesite.config;

import java.sql.DatabaseMetaData;
import java.sql.Connection;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class LegacyPulsarMigration implements ApplicationRunner {
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public LegacyPulsarMigration(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!tableExists("pulsars")) {
            return;
        }
        jdbcTemplate.update("""
                insert into orbits (
                    public_id, user_id, encrypted_title, encrypted_description,
                    kind, flexibility, target_minutes_per_week,
                    minimum_session_minutes, maximum_session_minutes,
                    energy_type, priority, category, color_key,
                    active, auto_schedule, created_at, updated_at, encryption_key_version
                )
                select
                    p.public_id, p.user_id, p.encrypted_title, p.encrypted_subject,
                    'PULSAR', 'FLEXIBLE', p.target_minutes_per_week,
                    30, 90, 'STUDY', 'MEDIUM', 'OTHER', 'violet',
                    p.active, true, p.created_at, p.updated_at, p.encryption_key_version
                from pulsars p
                where not exists (
                    select 1 from orbits o where o.public_id = p.public_id
                )
                """);
    }

    private boolean tableExists(String tableName) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            try (ResultSet tables = metadata.getTables(null, null, tableName, new String[]{"TABLE"})) {
                if (tables.next()) {
                    return true;
                }
            }
            try (ResultSet tables = metadata.getTables(null, null, tableName.toUpperCase(), new String[]{"TABLE"})) {
                return tables.next();
            }
        }
    }
}
