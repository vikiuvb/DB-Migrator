package com.chargebee.shard_migrator;

/*
 * Copyright (c) 2017 ChargeBee Inc
 * All Rights Reserved.
 */
/**
 *
 * @author vignesh
 */
public class ShardExecResult {

    public enum State {

        not_started, running, error, unknown, completed;
    }
    public State state = State.not_started;
    public Exception ex;
}
