package models;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.*;

import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import annotations.Record;

@Record(collectionName = "contract", primaryKey = "optionSymbol")
public class Contract extends Persistable {
	int securityType;
	String symbol;
	Date expirationDate;
	String optionSymbol;
	String putOrCall;
	Float strikePrice;
	
	public String getPutOrCall() {
		return putOrCall;
	}

	public void setPutOrCall(String putOrCall) {
		this.putOrCall = putOrCall;
	}

	public Float getStrikePrice() {
		return strikePrice;
	}

	public void setStrikePrice(Float strikePrice) {
		this.strikePrice = strikePrice;
	}	
	public Contract() {
		
	}
	
	public Contract(String optionSymbol) throws ParseException {
		this.optionSymbol = optionSymbol;
		String regex = "([A-Z]+)([\\d]+)([A-Z])([\\d]+)";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(optionSymbol);
		SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd");
		if (matcher.matches()) {
			this.symbol = matcher.group(1);
			this.expirationDate = sdf.parse(matcher.group(2));
			this.putOrCall = matcher.group(3);
			String price = matcher.group(4).replaceFirst("^0+(?!$)", "");
			price = price.substring(0, price.length() - 3) + "." + price.substring(price.length() - 3);
			this.strikePrice = Float.valueOf(price);
		}
	}
	
	public Date getExpirationDate() {
		return expirationDate;
	}
	public void setExpirationDate(Date expirationDate) {
		this.expirationDate = expirationDate;
	}
	public String getOptionSymbol() {
		return optionSymbol;
	}
	public void setOptionSymbol(String optionSymbol) {
		this.optionSymbol = optionSymbol;
	}
	public int getSecurityType() {
		return securityType;
	}
	public void setSecurityType(int securityType) {
		this.securityType = securityType;
	}
	public String getSymbol() {
		return symbol;
	}
	public void setSymbol(String symbol) {
		if (this.optionSymbol == null)
			this.optionSymbol = symbol;
		
		this.symbol = symbol;
	}
	
	@Override
	public DBObject toDocument() {
		DBObject contractDoc = new BasicDBObject();
		contractDoc.put("symbol", this.symbol);
		contractDoc.put("securityType", this.securityType);
		contractDoc.put("expirationDate", this.expirationDate);
		contractDoc.put("optionSymbol", this.optionSymbol);
		contractDoc.put("strikePrice", this.strikePrice);
		contractDoc.put("putOrCall", this.putOrCall);
		return contractDoc;
	}
}
