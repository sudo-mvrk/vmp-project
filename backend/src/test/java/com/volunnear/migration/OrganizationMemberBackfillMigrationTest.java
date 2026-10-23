package com.volunnear.migration;

import com.volunnear.support.AbstractIntegrationTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * V2 on a database that already has organizations with the old shared id:
 * each one gets its OWNER member, and new organizations get ids after the existing ones.
 * Runs in its own database of the shared container, so the application schema is not touched.
 */
class OrganizationMemberBackfillMigrationTest extends AbstractIntegrationTest {
    private static final String DATABASE = "v2_backfill_check";

    @Test
    void v2BackfillsOwnerMembersForExistingOrganizations() throws Exception {
        try (Connection admin = connect(POSTGRES.getJdbcUrl()); Statement statement = admin.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS " + DATABASE);
            statement.execute("CREATE DATABASE " + DATABASE);
        }
        String url = POSTGRES.getJdbcUrl().replace("/" + POSTGRES.getDatabaseName(), "/" + DATABASE);

        flyway(url, "1").migrate();
        try (Connection connection = connect(url); Statement statement = connection.createStatement()) {
            statement.execute("INSERT INTO app_users (id, username) VALUES (7, 'oldOrg'), (8, 'oldVol')");
            statement.execute("INSERT INTO organizations (id, organization_name) VALUES (7, 'Old org')");
        }

        flyway(url, "2").migrate();

        try (Connection connection = connect(url); Statement statement = connection.createStatement()) {
            ResultSet members = statement.executeQuery("SELECT organization_id, user_id, role FROM organization_member");
            assertThat(members.next()).isTrue();
            assertThat(members.getLong("organization_id")).isEqualTo(7);
            assertThat(members.getLong("user_id")).isEqualTo(7);
            assertThat(members.getString("role")).isEqualTo("OWNER");
            assertThat(members.next()).isFalse();

            ResultSet inserted = statement.executeQuery(
                    "INSERT INTO organizations (organization_name) VALUES ('New org') RETURNING id");
            inserted.next();
            assertThat(inserted.getLong("id")).isGreaterThan(7);
        }
    }

    private static Flyway flyway(String url, String target) {
        return Flyway.configure()
                .dataSource(url, POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .target(target)
                .load();
    }

    private static Connection connect(String url) throws Exception {
        return DriverManager.getConnection(url, POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
