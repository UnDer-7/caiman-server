package com.caimanproject.app.config;

import com.caimanproject.app.property.CaimanServerPropsConfig;
import com.caimanproject.contracts.exception.LogField;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.hibernate.community.dialect.SQLiteDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;

@Slf4j
@Configuration(proxyBeanMethods = false)
public class DataSourceConfig {

    @Bean
    public DataSource dataSource(CaimanServerPropsConfig props) {
        final var type = props.database().type();
        final var dataSource = switch (type) {
            case POSTGRES -> buildPostgresDataSource(
                props.database().url(),
                props.database().username(),
                props.database().password()
            );
            case SQLITE -> buildSqliteDataSource(props.database().sqliteFile());
        };
        log.info(
            LogField.Placeholders.TWO.getPlaceholder(),
            StructuredArguments.kv(LogField.MSG.label(), "DataSource initialized"),
            StructuredArguments.kv(LogField.DATABASE_TYPE.label(), type.name())
        );
        return dataSource;
    }

    @Bean
    public JpaVendorAdapter jpaVendorAdapter(CaimanServerPropsConfig props) {
        var adapter = new HibernateJpaVendorAdapter();
        adapter.setDatabasePlatform(switch (props.database().type()) {
            case POSTGRES -> PostgreSQLDialect.class.getName();
            case SQLITE   -> SQLiteDialect.class.getName();
        });
        adapter.setGenerateDdl(false);
        return adapter;
    }

    private DataSource buildPostgresDataSource(String url, String username, String password) {
        var config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        return new HikariDataSource(config);
    }

    private DataSource buildSqliteDataSource(String sqliteFile) {
        var config = new SQLiteConfig();
        config.setJournalMode(SQLiteConfig.JournalMode.WAL);
        config.setBusyTimeout(5000);
        config.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);
        config.setTempStore(SQLiteConfig.TempStore.MEMORY);
        config.setCacheSize(-2000);
        config.enforceForeignKeys(true);

        var ds = new SQLiteDataSource(config);
        ds.setUrl("jdbc:sqlite:" + sqliteFile);
        return ds;
    }
}
