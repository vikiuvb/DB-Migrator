/*
 * Copyright (c) 2017 ChargeBee Inc
 * All Rights Reserved.
 */
package com.chargebee.shard_migrator;

/**
 *
 * @author vignesh
 */
public class Shard {

    public String name;
    public String dbname;
    public String hostname;
    public Integer port;
    public String username;
    public String password;

    public String dbServerId() {
        return hostname + ":" + port;
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        } else if (that == null) {
            return false;
        } else if (!getClass().equals(that.getClass())) {
            return false;
        } else {
            Shard other = (Shard) that;
            return this.name.equals(other.name);
        }
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "Shard[" + name + "]";
    }
}
