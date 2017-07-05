/*
 * Copyright (c) 2017 ChargeBee Inc
 * All Rights Reserved.
 */
package com.chargebee.shard_migrator.config;

import com.chargebee.shard_migrator.sql.JdbcDriver;
import com.chargebee.shard_migrator.intf.*;

/**
 *
 * @author vignesh
 */
public class Config {

    public JdbcDriver jdbcDriver;

    public ShardDetailsProvider shardProvider;

    public ChangeScriptProvider scriptProvider;

    public boolean concurrentRun = false;

    public boolean ignoreErrors = false;
}
