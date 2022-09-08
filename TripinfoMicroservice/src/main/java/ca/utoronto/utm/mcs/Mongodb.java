package ca.utoronto.utm.mcs;


import com.mongodb.*;
import com.mongodb.client.*;
import com.mongodb.MongoClient;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class Mongodb {

    private MongoClient mongoClient;
    private MongoDatabase db;
    private MongoCollection<Document> col;

    public Mongodb() throws Exception {
        try {
             MongoClientOptions option = MongoClientOptions.builder().build();
            MongoClientURI mongoURI = new MongoClientURI("mongodb://root:123456@mongodb:27017/trip?authSource=admin");
            this.mongoClient = new MongoClient(mongoURI);
            this.db = this.mongoClient.getDatabase("trip");
            this.col = this.db.getCollection("trips");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Find corresponding document by the driver uid and passenger uid
     * @param driver String driver uid
     * @param passenger String passenger uid
     * @param startTime String start time
     * @return boolean
     */
    public boolean findByDriverPassenger(String driver, String passenger, String startTime) {
        Document query = new Document("driver",driver)
                .append("passenger",passenger)
                .append("startTime",startTime);

        MongoCursor<Document> mongoCursor = this.col.find(query).cursor();
        return mongoCursor.hasNext();
    }

    /**
     * Given the driver uid, the passenger uid, and the startTime create a document in the collection
     * @param driver String driver uid
     * @param passenger String passenger uid
     * @param startTime String unix timestamp format
     * @return BasicDBObject
     */
    public Document confirmCreateDocument(String driver, String passenger, String startTime) {
        Document doc = new Document("driver",driver)
                .append("passenger",passenger)
                .append("startTime",startTime)
                .append("distance","")
                .append("totalCost","")
                .append("endTime","")
                .append("timeElapsed","")
                .append("driverPayout","")
                .append("discount","");
        this.col.insertOne(doc);
        return doc;
    }

    /**
     * Given the driver uid, the passenger uid, and the startTime delete corresponding document in the collection
     * @param driver String driver uid
     * @param passenger String passenger uid
     * @param startTime String unix timestamp format
     */
    public void deleteDocument(String driver, String passenger, String startTime) {
        Document doc = new Document("driver",driver)
                .append("passenger",passenger)
                .append("startTime",startTime);
        this.col.deleteOne(doc);
    }

    /**
     * Given object id return the corresponding object
     * @param id String object id
     * @return DBOBject
     */
    public Document findById(String id) {
        Document doc = new Document("_id",new ObjectId(id));
        FindIterable<Document> findIterable =  this.col.find(doc);
        MongoCursor<Document> mongoCursor = findIterable.iterator();
        if(mongoCursor.hasNext()){
            return mongoCursor.next();
        }
        return null;
    }

    /**
     * Given object id and check exist
     * @param id String object id
     * @return Boolean
     */
    public Boolean findById_boolean(String id) {
        Document doc = new Document("_id",new ObjectId(id));
        FindIterable<Document> findIterable =  this.col.find(doc);
        MongoCursor<Document> mongoCursor = findIterable.iterator();
        return mongoCursor.hasNext();
    }

    /**
     * Update the document
     * @param id The ObjectId of the document
     * @param distance The distance of the trip
     * @param endTime The end time of the trip
     * @param timeElapsed The time spend for the entire trip (EndTime - StartTime)
     * @param discount The discount for this trip
     * @param totalCost The total cost of the trip before any discount
     * @param driverPayout The money that earned by driver, it is 65% of the totalCost
     * @param startTime The start time of the trip
     * @param passenger The uid of the passenger in this trip
     * @param driver The uid of the driver in this trip
     */
    public void updateDocument(String id, String distance, String endTime, String timeElapsed,
                               String discount, String totalCost, String driverPayout,
                               String startTime, String passenger, String driver) {
        Document query = new Document("_id", new ObjectId(id));
        Document up = new Document("$set",
                new Document("_id", new ObjectId(id))
                        .append("distance", distance)
                        .append("endTime", endTime)
                        .append("timeElapsed", timeElapsed)
                        .append("discount", discount)
                        .append("totalCost", totalCost)
                        .append("driverPayout", driverPayout)
                        .append("startTime", startTime)
                        .append("passenger", passenger)
                        .append("driver", driver));
        this.col.findOneAndUpdate(query,up);
    }

    /**
     * Check whether a passenger is in the database
     * @param passenger String passenger uid
     * @return boolean
     */
    public boolean findPassenger(String passenger) {
        Document query = new Document("passenger", passenger);
        FindIterable<Document> findIterable = this.col.find(query);
        MongoCursor<Document> mongoCursor = findIterable.iterator();
        return mongoCursor.hasNext();
    }

    /**
     * Get all the trips given passenger uid
     * @param passenger String passenger uid
     * @return ArrayList
     * @throws JSONException JSONException
     */
    public ArrayList<JSONObject> findTripPassenger(String passenger) throws JSONException {
        Document query = new Document("passenger", passenger);
        FindIterable<Document> findIterable = this.col.find(query);
        MongoCursor<Document> mongoCursor = findIterable.iterator();
        ArrayList<JSONObject> allTrips = new ArrayList<>();
        while(mongoCursor.hasNext()){
            Document object = mongoCursor.next();
            String id = object.get("_id").toString();
            String distance = object.get("distance").toString();
            String totalCost = object.get("totalCost").toString();
            String discount = object.get("discount").toString();
            String startTime = object.get("startTime").toString();
            String endTime = object.get("endTime").toString();
            String timeElapsed = object.get("timeElapsed").toString();
            String driver = object.get("driver").toString();

            JSONObject trip = new JSONObject();
            trip.put("_id", id);
            trip.put("distance", distance);
            trip.put("totalCost", totalCost);
            trip.put("discount", discount);
            trip.put("startTime", startTime);
            trip.put("endTime", endTime);
            trip.put("timeElapsed", timeElapsed);
            trip.put("driver", driver);

            allTrips.add(trip);
        }
        return allTrips;
    }

    /**
     * Check whether a driver is in the database
     * @param driver String driver uid
     * @return boolean
     */
    public boolean findDriver(String driver) {
        Document query = new Document("driver", driver);
        FindIterable<Document> findIterable = this.col.find(query);
        MongoCursor<Document> mongoCursor = findIterable.iterator();
        return mongoCursor.hasNext();
    }

    /**
     * Get all the trips given passenger uid
     * @param driver String passenger uid
     * @return ArrayList
     * @throws JSONException JSONException
     */
    public ArrayList<JSONObject> findTripDriver(String driver) throws JSONException {
        Document query = new Document("driver", driver);
        FindIterable<Document> findIterable = this.col.find(query);
        MongoCursor<Document> mongoCursor = findIterable.iterator();
        ArrayList<JSONObject> allTrips = new ArrayList<>();
        while(mongoCursor.hasNext()){
            Document object = mongoCursor.next();
            String id = object.get("_id").toString();
            String distance = object.get("distance").toString();
            String driverPayout = object.get("driverPayout").toString();
            String startTime = object.get("startTime").toString();
            String endTime = object.get("endTime").toString();
            String timeElapsed = object.get("timeElapsed").toString();
            String passenger = object.get("passenger").toString();

            JSONObject trip = new JSONObject();
            trip.put("_id", id);
            trip.put("distance", distance);
            trip.put("driverPayout", driverPayout);
            trip.put("startTime", startTime);
            trip.put("endTime", endTime);
            trip.put("timeElapsed", timeElapsed);
            trip.put("passenger", passenger);

            allTrips.add(trip);
        }
        return allTrips;
    }
}