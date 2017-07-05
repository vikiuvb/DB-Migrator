/*
 * Copyright (c) 2017 ChargeBee Inc
 * All Rights Reserved.
 */
package com.chargebee.shard_migrator.intf;

import com.chargebee.shard_migrator.Shard;
import java.util.*;

/**
 *
 * @author vignesh
 */
public interface ShardDetailsProvider {

    List<Shard> getShardDetails();
}
