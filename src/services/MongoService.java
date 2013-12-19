package services;

import java.lang.reflect.Field;
import java.net.UnknownHostException;

import org.bson.types.ObjectId;

import models.Persistable;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.DB;
import com.mongodb.WriteResult;

import annotations.Record;

public class MongoService {
	private static MongoService instance = null;
	private DB database = null;
	
	protected MongoService() throws UnknownHostException {
		MongoClient mongoClient = new MongoClient("localhost", 27017);
		database = mongoClient.getDB("quotes");
	}
	
	public static MongoService getInstance() throws UnknownHostException {
		if (instance == null) {
			instance = new MongoService();
		}
		return instance;
	}
	
	public void save(Persistable obj) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		Record recAnnotation = obj.getClass().getAnnotation(Record.class);
		String collectionName = recAnnotation.collectionName();
		Object keyValue = null;
		if (!recAnnotation.primaryKey().equals("_id")) {
			Field primaryKeyField = obj.getClass().getDeclaredField(recAnnotation.primaryKey());
			primaryKeyField.setAccessible(true);
			keyValue = primaryKeyField.get(obj);
		} else {
			keyValue = obj.getObjectId();
		}
		
		DBCollection collection = database.getCollection(collectionName);
		DBObject doc = obj.toDocument();
		
		if (keyValue != null) {
			BasicDBObject query = new BasicDBObject(recAnnotation.primaryKey(), keyValue);
			WriteResult result = collection.update(query, doc);
			int numAffected = result.getN();
			
			if (numAffected > 0) {
				ObjectId mongoId = (ObjectId) collection.findOne(query).get("_id");
				obj.setObjectId(mongoId);
				return;
			}
		} 
		
		
		collection.save(doc);
		obj.setObjectId((ObjectId) doc.get("_id"));
	}
}
