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

    // SQLite server-optimized config. Reference: https://kerkour.com/sqlite-for-servers
    private DataSource buildSqliteDataSource(String sqliteFile) {
        var config = new SQLiteConfig();
        // Allows concurrent reads while a write is in progress (readers don't block writers)
        config.setJournalMode(SQLiteConfig.JournalMode.WAL);
        // Wait up to 5s before returning SQLITE_BUSY when DB is locked by another connection
        config.setBusyTimeout(5000);
        // Sync at critical moments only; safe with WAL and better performance than FULL
        config.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);
        // Store temp tables/indexes in RAM instead of disk
        config.setTempStore(SQLiteConfig.TempStore.MEMORY);
        // Max ~200MB RAM used to cache database pages (lazy allocation, not reserved upfront)
        config.setCacheSize(-200000);
        // Acquire write lock at transaction start; ensures busy_timeout is respected on writes
        config.setTransactionMode(SQLiteConfig.TransactionMode.IMMEDIATE);
        // SQLite does not enforce FK constraints by default; enable explicitly
        config.enforceForeignKeys(true);

        var ds = new SQLiteDataSource(config);
        ds.setUrl("jdbc:sqlite:" + sqliteFile);
        return ds;
    }
}
