package quotes;

import java.io.IOException;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;

import models.Contract;

public interface QuoteSource {
	void subscribe(Collection<Contract> contracts) throws UnknownHostException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException;
	void subscribeATM(Collection<String> contracts, Date endDate) throws Exception;
	void run();
	void stop();
}
