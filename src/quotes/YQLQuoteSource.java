package quotes;

import models.Contract;
import models.QuoteTick;

import org.jsoup.Jsoup;

import java.io.*;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpEntity;
import org.apache.http.client.*;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bson.types.ObjectId;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import parsers.XmlParser;
import services.MongoService;

import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBCursor;
import com.mongodb.ServerAddress;

public class YQLQuoteSource implements QuoteSource {
	
	private Map<String, ObjectId> contractMap = new HashMap<String, ObjectId>();
	private Map<String, Collection<Contract>> atmOptions = new HashMap<String, Collection<Contract>>();
	private Map<String, ObjectId> optionMap = new HashMap<String, ObjectId>();
	private DB quoteDb;
	private int interval;
	
	public YQLQuoteSource(int interval) throws UnknownHostException {
		MongoClient mongoClient = new MongoClient("localhost", 27017);
		quoteDb = mongoClient.getDB("quotes");
		this.interval = interval;
	}
	
	private void queryATMOptions() throws ClientProtocolException, IOException, ParserConfigurationException, SAXException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		// figure out what is the ATM options
		for (String symbol : atmOptions.keySet()) {
			DBCollection quoteColl = quoteDb.getCollection("contractQuote");
			BasicDBObject query = new BasicDBObject("contractId", this.contractMap.get(symbol));
			
			DBCursor cursor = quoteColl.find(query).sort(new BasicDBObject("timestamp", -1)).limit(1);
			DBObject dbContract = cursor.next();
			Double quote = ((Double) dbContract.get("ask") + (Double) dbContract.get("bid")) / 2;
			Float threshold = (quote).floatValue() * 0.05f;
			
			List<Contract> atmContract = new ArrayList<Contract>();
			String yqlString = ""; 
			boolean first = true; 
			for (Contract contract : atmOptions.get(symbol)) {
				if (Math.abs(quote - contract.getStrikePrice()) < threshold) {
					atmContract.add(contract);
					
					if (first) {
						yqlString = "'" + contract.getOptionSymbol() + "'";
						first = false;
					} else {
						yqlString = yqlString + ", '" + contract.getOptionSymbol() + "'";
					}
				}
			}
			
			String yqlQuery = "use \"store://spHclBldle1Fcn1EWfxOZs\" as yahoo.finance.oquote2; SELECT * FROM yahoo.finance.oquote2 WHERE symbol IN (" + yqlString + ")";
			String yqlBase = "http://query.yahooapis.com/v1/public/yql?q=" + URLEncoder.encode(yqlQuery, "utf-8");
			Document doc = XmlParser.getInstance().getDocument(yqlBase);
	        NodeList nl = doc.getElementsByTagName("option");
	        MongoService mongoService = MongoService.getInstance();
	        
	        for(int i = 0; i < nl.getLength(); i++) {
	            if (nl.item(i).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
	                 Element nameElement = (Element) nl.item(i);
	                 String optionSym = nameElement.getAttribute("sym");
	                 Element bidElement = (Element) nameElement.getElementsByTagName("bid").item(0);
	                 String bid = bidElement.getTextContent();
	                 Element askElement = (Element) nameElement.getElementsByTagName("ask").item(0);
	                 String ask = askElement.getTextContent();
	                 
	                 QuoteTick quoteTick = new QuoteTick();
	                 try {
		                 quoteTick.setAsk(Float.valueOf(ask));
		                 quoteTick.setBid(Float.valueOf(bid));
	                 } catch (NumberFormatException e) {
	                	 // do nothing
	                 }
	                 
	                 quoteTick.setTimestamp(new Date());
	                 quoteTick.setContractId(this.optionMap.get(optionSym));
	                 
	                 mongoService.save(quoteTick);
	             }
	        }
		}
	}
	
	private void queryYQL() throws ClientProtocolException, IOException, ParserConfigurationException, SAXException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		String yqlRequest = "http://query.yahooapis.com/v1/public/yql?q="; 
		String symbolRequest = "";
		boolean first = true;
		for (String symbol : contractMap.keySet()) {
			if (first) {
				symbolRequest = "'" + symbol + "'";
				first = false;
			} else {
				symbolRequest = symbolRequest + ", '" + symbol + "'";
			}
		}
		
		String yql = "Select symbol, Bid, Ask from yahoo.finance.quotes where symbol IN (" + symbolRequest + ")";
		yqlRequest = yqlRequest + URLEncoder.encode(yql, "utf-8") + "&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys";
		Document doc = XmlParser.getInstance().getDocument(yqlRequest);
        NodeList nl = doc.getElementsByTagName("quote");
        MongoService mongoService = MongoService.getInstance();
        
        for(int i = 0; i < nl.getLength(); i++) {
            if (nl.item(i).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                 Element nameElement = (Element) nl.item(i);
                 String symbol = nameElement.getAttribute("symbol");
                 Element bidElement = (Element) nameElement.getElementsByTagName("Bid").item(0);
                 String bid = bidElement.getTextContent();
                 Element askElement = (Element) nameElement.getElementsByTagName("Ask").item(0);
                 String ask = askElement.getTextContent();
                 
                 QuoteTick quoteTick = new QuoteTick();
                 quoteTick.setAsk(Float.valueOf(ask));
                 quoteTick.setBid(Float.valueOf(bid));
                 quoteTick.setTimestamp(new Date());
                 quoteTick.setContractId(contractMap.get(symbol));
                 
                 mongoService.save(quoteTick);
             }
        }
		
	}
	
	public static void main(String[] args) throws Exception {
		Contract contract = new Contract();
		contract.setSymbol("MSFT");
		contract.setSecurityType(0);
		List<Contract> contracts = new ArrayList<Contract>();
		List<String> atmOptionContracts = new ArrayList<String>();
		contracts.add(contract);
		atmOptionContracts.add("MSFT");
		
		Contract googContract = new Contract();
		googContract.setSymbol("GOOG");
		googContract.setSecurityType(0);
		contracts.add(googContract);
		atmOptionContracts.add("GOOG");
		
		QuoteSource quoteSource = new YQLQuoteSource(1000 * 60 * 30);
		quoteSource.subscribe(contracts);
		
		Date threeMonthsExpiration = new Date();
		Calendar cal = Calendar.getInstance(); 
		cal.setTime(threeMonthsExpiration);
		cal.add(Calendar.MONTH, 3);
		threeMonthsExpiration = cal.getTime();
		
		quoteSource.subscribeATM(atmOptionContracts, threeMonthsExpiration);
		
		quoteSource.run();
	}
	
	@Override
	public void subscribe(Collection<Contract> contracts) throws UnknownHostException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		MongoService mongoService = MongoService.getInstance();
		for (Contract contract : contracts) { 
			mongoService.save(contract);
			contractMap.put(contract.getSymbol(), contract.getObjectId());
		}
	}

	@Override
	public void run() {
		Timer timer = new Timer();
		final YQLQuoteSource self = this;
		timer.schedule(new TimerTask() {
			public void run() {
				try {
					self.queryYQL();
					self.queryATMOptions();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}, new Date(), interval);
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void subscribeATM(Collection<String> symbols, Date endDate) throws Exception {
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		cal.set(Calendar.DAY_OF_MONTH, 1); 
		Date currentDate = cal.getTime(); // get the beginning of the month
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM");
		
		for (String contractSymbol : symbols) {
			List<Contract> contracts = new ArrayList<Contract>();
			this.atmOptions.put(contractSymbol, contracts);
			MongoService mongoService = MongoService.getInstance();
			while (currentDate.before(endDate)) {
				String formatQueryString = sdf.format(currentDate);
				String yahooFinanceString = "http://finance.yahoo.com/q/op?s=" + contractSymbol + "&m=" + formatQueryString;
				
				org.jsoup.nodes.Document doc = Jsoup.connect(yahooFinanceString).get();
				org.jsoup.select.Elements optionTables = doc.select(".yfnc_datamodoutline1");
				org.jsoup.select.Elements tdElements = optionTables.select("td[nowrap]");
				
				for (org.jsoup.nodes.Element element : tdElements) {
					Contract optionContract = new Contract(element.nextElementSibling().select("a").text()); 
					mongoService.save(optionContract);
					contracts.add(optionContract);
					this.optionMap.put(optionContract.getOptionSymbol(), optionContract.getObjectId());
				}
				
				cal.setTime(currentDate);
				cal.add(Calendar.MONTH, 1);
				currentDate  = cal.getTime();
			}
		}
	}
}
