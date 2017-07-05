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
public class QueryExecutor {

    private final Connection connection;

    public QueryExecutor(JdbcDriver driver, Shard shard) throws SQLException {
        this.connection = driver.getConnection(shard);
    }

    public void execute(String sql) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

}
