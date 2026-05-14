package com.trackcal.trackcalserver.config;

import com.mongodb.MongoNamespace;
import com.trackcal.trackcalserver.model.User;
import com.trackcal.trackcalserver.repository.UserRepository;
import org.bson.Document;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DatabaseMigrationRunner implements ApplicationRunner {
    private final MongoTemplate mongoTemplate;
    private final UserRepository userRepository;

    public DatabaseMigrationRunner(MongoTemplate mongoTemplate, UserRepository userRepository) {
        this.mongoTemplate = mongoTemplate;
        this.userRepository = userRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        renameCollection("user_profiles", "user_details");
        renameCollection("meal_entries", "meals");
        renameCollection("analytics_entries", "analytics");

        migrateUserReference("user_details");
        migrateUserReference("meals");
        migrateUserReference("analytics");
        migrateDailyDeficitField();
    }

    private void renameCollection(String oldName, String newName) {
        if (!mongoTemplate.collectionExists(oldName) || mongoTemplate.collectionExists(newName)) {
            return;
        }

        MongoNamespace namespace = new MongoNamespace(mongoTemplate.getDb().getName(), newName);
        mongoTemplate.getCollection(oldName).renameCollection(namespace);
    }

    private void migrateUserReference(String collectionName) {
        if (!mongoTemplate.collectionExists(collectionName)) {
            return;
        }

        Query query = Query.query(Criteria.where("userEmail").exists(true));
        List<Document> documents = mongoTemplate.find(query, Document.class, collectionName);

        for (Document document : documents) {
            String email = document.getString("userEmail");
            Object id = document.get("_id");
            User user = email == null ? null : userRepository.findByEmail(email).orElse(null);
            Query documentQuery = Query.query(Criteria.where("_id").is(id));

            if (user == null) {
                mongoTemplate.remove(documentQuery, collectionName);
                continue;
            }

            Update update = new Update()
                    .set("userId", user.getId())
                    .unset("userEmail");
            mongoTemplate.updateFirst(documentQuery, update, collectionName);
        }
    }

    private void migrateDailyDeficitField() {
        if (!mongoTemplate.collectionExists("user_details")) {
            return;
        }

        Query query = Query.query(Criteria.where("dailyDeficit").exists(true));
        List<Document> documents = mongoTemplate.find(query, Document.class, "user_details");

        for (Document document : documents) {
            Object id = document.get("_id");
            Object value = document.get("dailyDeficit");
            Query documentQuery = Query.query(Criteria.where("_id").is(id));
            Update update = new Update().unset("dailyDeficit");

            if (!document.containsKey("dailyCalorieAdjustment") && value instanceof Number number) {
                update.set("dailyCalorieAdjustment", Math.abs(number.intValue()));
            }

            mongoTemplate.updateFirst(documentQuery, update, "user_details");
        }
    }
}
