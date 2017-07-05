/*
 * Copyright (c) 2017 ChargeBee Inc
 * All Rights Reserved.
 */
package com.chargebee.shard_migrator.sql;

import com.chargebee.shard_migrator.Shard;
import java.sql.*;

/**
 *
 * @author vignesh
 */
public enum JdbcDriver {

    MYSQL("com.mysql.jdbc.Driver") {

        @Override
        public Connection getConnection(Shard shard) throws SQLException {
            String connectionUrl = "jdbc:mysql://" + shard.hostname + ":" + shard.port + "/" + shard.dbname;
            return DriverManager.getConnection(connectionUrl, shard.username, shard.password);
        }
    },
    
    POSTGRE_SQL("org.postgresql.Driver") {

        @Override
        public Connection getConnection(Shard shard) throws SQLException {
            String connectionUrl = "jdbc:postgresql://" + shard.hostname + ":" + shard.port + "/" + shard.dbname;
            return DriverManager.getConnection(connectionUrl, shard.username, shard.password);
        }
    };

    private JdbcDriver(String driverClz) {
        this.driverClz = driverClz;
    }

    public final String driverClz;

    public abstract Connection getConnection(Shard shard) throws SQLException;
}
