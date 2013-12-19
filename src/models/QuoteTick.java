package models;

import java.util.Date;

import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import annotations.Record;

@Record(collectionName = "contractQuote", primaryKey = "_id")
public class QuoteTick extends Persistable {
	float bid;
	float ask; 
	ObjectId contractId;
	Date timestamp;
	
	@Override
	public DBObject toDocument() {
		DBObject quoteTickDoc = new BasicDBObject();
		quoteTickDoc.put("bid", this.bid);
		quoteTickDoc.put("ask", this.ask);
		quoteTickDoc.put("contractId", this.contractId);
		quoteTickDoc.put("timestamp", this.timestamp);
		return quoteTickDoc;
	}
	
	public float getBid() {
		return bid;
	}
	public void setBid(float bid) {
		this.bid = bid;
	}
	public float getAsk() {
		return ask;
	}
	public void setAsk(float ask) {
		this.ask = ask;
	}
	public ObjectId getContractId() {
		return contractId;
	}
	public void setContractId(ObjectId contractId) {
		this.contractId = contractId;
	}
	public Date getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
}
