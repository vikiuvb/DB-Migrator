/*
 * Copyright (c) 2017 ChargeBee Inc
 * All Rights Reserved.
 */
package com.chargebee.shard_migrator.intf;

import com.chargebee.shard_migrator.Shard;

/**
 *
 * @author vignesh
 */
public interface ChangeScriptProvider {

    String getChangeScript(Shard shard);
}
