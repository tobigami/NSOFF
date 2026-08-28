package com.nsoz.db.mongodb;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.connection.ConnectionPoolSettings;
import com.nsoz.server.Config;
import org.apache.log4j.Level;
import org.bson.Document;
import org.apache.log4j.Logger;
import org.bson.conversions.Bson;

import java.util.concurrent.TimeUnit;

// auto xóa lịch sử mongodb

public class MongoDbConnection {

    public static MongoClient mongoClient;
    public static MongoDatabase db;

    public static void connect() {
    Logger logger = Logger.getLogger("org.mongodb.driver");
        logger.setLevel(Level.OFF);
        Config config = Config.getInstance();
        mongoClient = MongoClients.create(config.getMongodbUrl());
        db = mongoClient.getDatabase(config.getMongodbName());
        deleteAllRecordsOlderThan7Day(db.getCollection("player"));
        deleteAllRecordsOlderThan7Day(db.getCollection("clone_player"));
        deleteAllRecordsOlderThan7Day(db.getCollection("history"));
        MongoCollection<Document> collection = getHistory();
        Bson index = new Document("time", 1);
        collection.createIndex(index);
    }

    public static  MongoCollection<Document> getCollection(String collectionName) {
        return db.getCollection(collectionName);
    }

    public static  MongoCollection<Document> getHistory() {
        return db.getCollection("history");
    }

    public static void deleteAllRecordsOlderThan7Day(MongoCollection<Document> collection) {
        // Set the cutoff date for records to delete
        long cutoff = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000);
        // Delete all records with an update_at field older than 7 days
        collection.deleteMany(Filters.lt("update_at", cutoff));
    }

    public static void close() {
        mongoClient.close();
    }

}
