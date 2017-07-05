/*
 * Copyright (c) 2017 ChargeBee Inc
 * All Rights Reserved.
 */
package com.chargebee.shard_migrator.sql;

/**
 *
 * @author vignesh
 */
public class SqlStmt {

    public int step;
    public String sql;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        } else if (that == null) {
            return false;
        } else if (!this.getClass().equals(that.getClass())) {
            return false;
        } else {
            return step == ((SqlStmt) that).step;
        }
    }

    @Override
    public int hashCode() {
        return step;
    }

    @Override
    public String toString() {
        return "SqlStmt{" + "step=" + step + ", sql=" + sql + '}';
    }
}
