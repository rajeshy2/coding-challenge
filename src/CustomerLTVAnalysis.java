import java.util.HashMap;

public class CustomerLTVAnalysis {
	
	static String PATH_TO_EVENTS_FILE = "./input/input.txt";
	static HashMap<String, CustomerDetails> custIdToCustDetails = new HashMap<>();
	
	public static void main(String[] args)
	{
		IngestEvents.ingest(PATH_TO_EVENTS_FILE,custIdToCustDetails);
		AnalyzeCustomerLTV.topXSimpleLTVCustomers(10, custIdToCustDetails);
	}
	
	

}
