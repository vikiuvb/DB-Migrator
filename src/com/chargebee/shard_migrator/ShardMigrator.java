/*
 * Copyright (c) 2017 ChargeBee Inc
 * All Rights Reserved.
 */
package com.chargebee.shard_migrator;

import java.sql.*;
import java.util.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.function.*;
import com.chargebee.shard_migrator.config.*;
import com.chargebee.shard_migrator.intf.*;
import com.chargebee.shard_migrator.sql.*;

/**
 *
 * @author vignesh
 */
public class ShardMigrator {

    private PrintStream out;
    private final Config config;
    private final Map<String, Queue<Shard>> workQueues;
    private final Map<Shard, ShardExecResult> resultMap;
    private volatile boolean shutdown = false;

    public ShardMigrator(Config config) {
        this.config = config;
        this.out = System.out;
        this.workQueues = new HashMap<>();
        this.resultMap = new HashMap<>();
        registerDriver(this.config.jdbcDriver);
    }

    public void setOut(PrintStream out) {
        this.out = out;
    }

    public void run() {
        runAsserts();
        loadShards();
        runMigrations();
        outputStatus();
        shutdown = true;
    }

    private void runAsserts() {
        if (shutdown) {
            throw new IllegalStateException("Migration is already completed");
        }
    }

    private void loadShards() {
        config.shardProvider.getShardDetails().forEach(shard -> {
            workQueues.putIfAbsent(shard.dbServerId(), new LinkedList<>());
            workQueues.get(shard.dbServerId()).add(shard);
            resultMap.put(shard, new ShardExecResult());
        });
    }

    private void runMigrations() {
        TaskExecutor executor = new TaskExecutor(config.concurrentRun ? workQueues.size() : 1);
        Runtime.getRuntime().addShutdownHook(new MigratorShutdownHook(executor));
        workQueues.forEach((dbId, queue) -> {
            executor.execute(new MigrationRunner(dbId, queue));
        });
        executor.shutdown();
    }

    private void outputStatus() {

        Map<ShardExecResult.State, List<Map.Entry<Shard, ShardExecResult>>> operationResult = groupByResultState(resultMap);
        Consumer<Map.Entry<Shard, ShardExecResult>> outBloc = (shard) -> out.println("\t" + shard.getKey().name);
        Consumer<Map.Entry<Shard, ShardExecResult>> outErrBloc = (shard) -> out.println("\t" + shard.getKey().name + " : " + shard.getValue().ex.getMessage());
        out.println();
        if (operationResult.get(ShardExecResult.State.completed).size() == resultMap.size()) {
            out.println("Migration completed successfully on all the shards\n");
        } else {

            if (!operationResult.get(ShardExecResult.State.error).isEmpty()) {
                out.println("The following shards had errors:");
                operationResult.get(ShardExecResult.State.error).forEach(outBloc);
                out.println();
                out.println("Errors:");
                operationResult.get(ShardExecResult.State.error).forEach(outErrBloc);
                out.println();
            }

            if (!operationResult.get(ShardExecResult.State.unknown).isEmpty() || !operationResult.get(ShardExecResult.State.running).isEmpty()) {
                out.println("The following shards are in unknown state:");
                operationResult.get(ShardExecResult.State.running).forEach(outBloc);
                operationResult.get(ShardExecResult.State.unknown).forEach(outBloc);
                out.println();
            }

            if (!operationResult.get(ShardExecResult.State.completed).isEmpty()) {
                out.println("Migration completed successfully on the following shards:");
                operationResult.get(ShardExecResult.State.completed).forEach(outBloc);
                out.println();
            }

            if (!operationResult.get(ShardExecResult.State.not_started).isEmpty()) {
                out.println("Migration was not attempted in the following shards:");
                operationResult.get(ShardExecResult.State.not_started).forEach(outBloc);
                out.println();

            }
        }
    }

    private class MigrationRunner implements Runnable {

        private final String dbServerId;
        private final Queue<Shard> queue;

        private MigrationRunner(String dbServerId, Queue<Shard> queue) {
            this.dbServerId = dbServerId;
            this.queue = queue;
        }

        @Override
        public void run() {
            out.println("Starting migration for " + dbServerId);
            while (!shutdown && !queue.isEmpty()) {
                Shard shard = queue.poll();
                ShardExecResult result = resultMap.get(shard);
                out.println("Running migration for " + shard);
                try {
                    result.state = ShardExecResult.State.running;
                    boolean success = runScript(shard, config.scriptProvider);
                    result.state = success ? ShardExecResult.State.completed : ShardExecResult.State.not_started;
                } catch (InterruptedException ex) {
                    out.println("Received interrupt, initiating soft shutdown");
                    result.state = ShardExecResult.State.unknown;
                    shutdown = true;
                    break;
                } catch (SQLException ex) {
                    result.state = ShardExecResult.State.error;
                    result.ex = ex;
                    if (!config.ignoreErrors) {
                        shutdown = true;
                        break;
                    }
                }
            }
        }

        private boolean runScript(Shard shard, ChangeScriptProvider scriptProvider) throws SQLException, InterruptedException {
            String sqlScript = scriptProvider.getChangeScript(shard);
            Set<SqlStmt> stmts = SqlParser.parseScript(sqlScript);
            if (stmts.isEmpty()) {
                out.println("No SQL script found for " + shard + ". Hence skipping..");
                return false;
            }
            boolean firstRun = true;
            for (SqlStmt stmt : stmts) {
                if (Thread.interrupted()) {
                    if (firstRun) {
                        return false;
                    } else {
                        throw new InterruptedException();
                    }
                }
                new QueryExecutor(config.jdbcDriver, shard).execute(stmt.sql);
                firstRun = false;
            }
            return true;
        }
    }

    private class MigratorShutdownHook extends Thread {

        private final Thread mainThread;
        private final TaskExecutor executor;

        public MigratorShutdownHook(TaskExecutor executor) {
            this.mainThread = Thread.currentThread();
            this.executor = executor;
        }

        @Override
        public void run() {
            executor.hardShutdown();
            shutdown = true;
            try {
                mainThread.join();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    private static void registerDriver(JdbcDriver driver) {
        try {
            Class.forName(driver.driverClz);
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException("Unable to register the jdbc driver : " + driver.driverClz, ex);
        }
    }

    private static Map<ShardExecResult.State, List<Map.Entry<Shard, ShardExecResult>>> groupByResultState(Map<Shard, ShardExecResult> resultMap) {
        Map<ShardExecResult.State, List<Map.Entry<Shard, ShardExecResult>>> grpMap = new EnumMap<>(ShardExecResult.State.class);
        for (ShardExecResult.State state : ShardExecResult.State.values()) {
            grpMap.put(state, new ArrayList<>());
        }
        for (Map.Entry<Shard, ShardExecResult> entry : resultMap.entrySet()) {
            grpMap.get(entry.getValue().state).add(entry);
        }
        return grpMap;
    }

    private static class TaskExecutor {

        private final ExecutorService executor;
        private final List<Future<?>> tasks;
        private volatile boolean hardShutdown = false;

        public TaskExecutor(int concurrencyLevel) {
            this.executor = Executors.newFixedThreadPool(concurrencyLevel);
            this.tasks = new ArrayList<>();
        }

        public Future<?> execute(Runnable command) {
            Future<?> future = executor.submit(command);
            tasks.add(future);
            return future;
        }

        public void hardShutdown() {
            hardShutdown = true;
        }

        public void shutdown() {
            while (!tasks.isEmpty()) {
                for (Iterator<Future<?>> iter = tasks.iterator(); iter.hasNext();) {
                    Future<?> task = iter.next();
                    if (task.isDone()) {
                        iter.remove();
                    } else {
                        if (hardShutdown) {
                            task.cancel(true);
                        }
                    }
                }
            }
            executor.shutdownNow();
        }
    }
}
