package com.documentbuilder.graph;

import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.dbms.api.DatabaseManagementService;

import java.nio.file.Path;

public class EmbeddedNeo4jManager {

    private static final String DATABASE_NAME = "CODE-SCHEMA";
    private static final Path DB_PATH = Path.of("data/graphdb");
    private static EmbeddedNeo4jManager instance;

    private final DatabaseManagementService managementService;
    private final GraphDatabaseService graphDb;

    private EmbeddedNeo4jManager() {
        managementService = new DatabaseManagementServiceBuilder(DB_PATH)
                .setConfig(GraphDatabaseSettings.auth_enabled, false) // Set to true for auth-enabled setups
                .build();
        graphDb = managementService.database(DATABASE_NAME);

        // Register shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                shutdown();
            } catch (Exception ignored) {}
        }));
    }

    public static synchronized EmbeddedNeo4jManager getInstance() {
        if (instance == null) {
            instance = new EmbeddedNeo4jManager();
        }
        return instance;
    }

    public GraphDatabaseService getConnection() {
        return graphDb;
    }

    public void shutdown() {
        managementService.shutdown();
    }
}

