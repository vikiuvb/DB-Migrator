/*
 * Copyright (c) 2017 ChargeBee Inc
 * All Rights Reserved.
 */
package com.chargebee.shard_migrator.sql;

import java.util.*;
import java.util.regex.*;
import com.chargebee.shard_migrator.sql.SqlStmt;

/**
 *
 * @author vignesh
 */
public class SqlParser {

    private static final Pattern convPat = Pattern.compile("\n#-(\\d+)-(.*?)\n#---", Pattern.DOTALL);

    public static Set<SqlStmt> parseScript(String script) {
        if (script == null) {
            return Collections.EMPTY_SET;
        }
         
        Set<SqlStmt> statements = new HashSet<>();
        SqlStmt stmt = new SqlStmt();
        stmt.step = 1;
        stmt.sql = script;
        statements.add(stmt);
//        Matcher m = convPat.matcher(script);
//        while (m.find()) {
//            SqlStmt stmt = new SqlStmt();
//            stmt.step = Integer.parseInt(m.group(1));
//            stmt.sql = m.group(2).trim();
//            if (stmt.sql.endsWith(";")) {
//                stmt.sql = stmt.sql.substring(0, stmt.sql.length() - 1);
//            }
//            if (statements.contains(stmt)) {
//                throw new RuntimeException("Duplicate statement " + stmt);
//            }
//            statements.add(stmt);
//        }
        return statements;
    }

}
