import java.awt.List;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.json.simple.JSONObject;

public class AnalyzeCustomerLTV {

	static HashMap<String, Double> ltvValsOfCustomers = new HashMap<>();

	static void topXSimpleLTVCustomers(int x, HashMap<String, CustomerDetails> custIdToCustDetails) {
		// If x > the number of customers in custIdToEvents (i.e.
		// custIdToEvents.size())
		// this function will return the max number of customers LTV present
		try {
			// same for all customers
			long totalNumberOfWeeks = getNumberOfWeeksForCurrentIngestion();
			for (String custId : custIdToCustDetails.keySet()) {
				double totalExpenditure = calcTotalExp(custIdToCustDetails.get(custId));
				int numberOfSiteVisits = custIdToCustDetails.get(custId).getVisitedDatesAndTime().size();
				/*
				 * 52(a) x t. Where a is the average customer value per week,
				 * (customer expenditures per visit (USD) x number of site
				 * visits per week) and t is the average customer lifespan. The
				 * average lifespan for Shutterfly is 10 years. 
				 * Customer expenditure per visit = totalExpenditure/totalNoOfVisits and
				 * number of site visits per week = totalNoOfVisits/totalNumberOfWeeks
				 * where totalNumberOfWeeks is the number of weeks(7day interval) between
				 * the start and end timeframe (info stored while ingesting the data)
				 * From the eqn: avg cust value 
				 *                     per week   = (totalExpenditure/totalNoOfVisits) * (totalNoOfVisits/totalNumberOfWeeks) 
				 * (canceling out totalNoOfVisits)=  totalExpenditure/totalNumberOfWeeks
				 * As expected in the problem, even though I am not using
				 * totalNoOfVisits in the calculation, the value is available.
				 */
				double avgCustValuePerWeek = totalExpenditure / ((totalNumberOfWeeks == 0) ? 1 : totalNumberOfWeeks);
				double ltvOfCustomer = 52 * avgCustValuePerWeek * 10;
				ltvValsOfCustomers.put(custId, ltvOfCustomer);
			}
			LinkedList<Entry<String, Double>> sortedCustLTV = sortCustLTVDesc();
			writeToFile(x, sortedCustLTV);
		} catch (Exception e) {
			// have hard coded the method name, other option is to use stack
			// trace or reflection to get method name
			Logger.logMessage(
					String.format("Exception in topXSimpleLTVCustomers method, Message : %s", e.getMessage()));
		}
	}

	static void writeToFile(int x, LinkedList<Entry<String, Double>> sortedCustLTV) {
		try{
			for (Entry<String, Double> entry : sortedCustLTV) {
				if (x > 0)
					Files.write(Paths.get("./output/output.txt"),
							String.format("%s : %.2f\n", entry.getKey(), entry.getValue()).getBytes()
							,StandardOpenOption.CREATE, StandardOpenOption.APPEND);
				--x;
			}
		}
		catch(Exception e)
		{
			Logger.logMessage(
					String.format("Exception in writeToFile method, Message : %s", e.getMessage()));
		}
	}

	static LinkedList<Entry<String, Double>> sortCustLTVDesc() {
		LinkedList<Entry<String, Double>> sortedCustLTV = null;
		try{
			sortedCustLTV = new LinkedList<Entry<String, Double>>(ltvValsOfCustomers.entrySet());

	        // Sorting the list based on values
	        Collections.sort(sortedCustLTV, new Comparator<Entry<String, Double>>()
	        {
	            public int compare(Entry<String, Double> cusLTVVal1,
	                    Entry<String, Double> cusLTVVal2)
	            {
	                return cusLTVVal2.getValue().compareTo(cusLTVVal1.getValue());
	            }
	        });
		}
		catch(Exception e)
		{
			Logger.logMessage(
					String.format("Exception in sortCustLTVDesc method, Message : %s", e.getMessage()));
		}
		return sortedCustLTV;
	}

	private static long getNumberOfWeeksForCurrentIngestion() {
		return ChronoUnit.WEEKS.between(IngestEvents.start, IngestEvents.end);
	}

	static double calcTotalExp(CustomerDetails customerDetails) {
		double totalExpenditure = 0;
		try {
			HashMap<String, JSONObject> orderEvents = customerDetails.getOrderDetails();
			for (String orderId : orderEvents.keySet()) {
				JSONObject orderDetails = orderEvents.get(orderId);
				String totalAmt = ((String) orderDetails.get("total_amount")).replaceAll("[^0-9\\.]+", "");
				totalExpenditure += (totalAmt != null && !totalAmt.isEmpty()) ? Double.parseDouble((totalAmt)) : 0;
			}
		} catch (Exception e) {
			Logger.logMessage(String.format("Exception in calcTotalExp method, Message : %s", e.getMessage()));
		}
		return totalExpenditure;
	}

}
