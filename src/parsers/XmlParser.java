package parsers;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XmlParser {
	private static XmlParser instance = null;
	
	protected XmlParser() {
	}
	
	public static XmlParser getInstance() {
		if (instance == null) {
			instance = new XmlParser();
		}
		return instance;
	}
	
	public Document getDocument(String url) throws ClientProtocolException, IOException, ParserConfigurationException, SAXException {
		CloseableHttpClient client = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet(url);
		CloseableHttpResponse response = client.execute(httpGet);
		
		HttpEntity entity = response.getEntity();
        String xmlString = EntityUtils.toString(entity);
        	        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = factory.newDocumentBuilder();
        InputSource inStream = new InputSource();
        inStream.setCharacterStream(new StringReader(xmlString));
        Document doc = db.parse(inStream);
        response.close();
		return doc;  
	}
}
