package models;

import org.bson.types.ObjectId;

import com.mongodb.DBObject;

import annotations.Record;

@Record(collectionName = "contract", primaryKey = "_id")
public class Persistable {
	ObjectId _id;
	
	public ObjectId getObjectId() {
		return _id;
	}

	public void setObjectId(ObjectId objectId) {
		this._id = objectId;
	}
	
	public DBObject toDocument() {
		return null;
	}
}
