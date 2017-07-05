import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HashSet;

import org.json.simple.JSONObject;

public class CustomerDetails {
	
	final String customerId;
	
	JSONObject customerInfo;
	
	HashMap<String, JSONObject> orderDetails = null;
	
	HashMap<String, JSONObject> siteVisits = null;
	
	HashMap<String, JSONObject> imageUploadEvents = null;
	
	HashSet<LocalDateTime> visitedDatesAndTime = null;
	
	public CustomerDetails(String custId)
	{
		this.customerId = custId;
		this.visitedDatesAndTime = new HashSet<>();
	}
	
	HashMap<String, JSONObject> getSiteVisits() {
		return siteVisits;
	}

	// only get is used as set visitedDatesAndTime is not required
	HashSet<LocalDateTime> getVisitedDatesAndTime() {
		return visitedDatesAndTime;
	}

	void setSiteVisits(HashMap<String, JSONObject> siteVisits) {
		this.siteVisits = siteVisits;
	}

	HashMap<String, JSONObject> getImageUploadEvents() {
		return imageUploadEvents;
	}

	void setImageUploadEvents(HashMap<String, JSONObject> imageUploadEvents) {
		this.imageUploadEvents = imageUploadEvents;
	}

	// no associated setter method because customer id is set during object initialization
	// and this is not expected to be changed or updated. This has also been implemented by 
	// using the final keyword for customerId instance variable
	String getCustomerId() {
		return customerId;
	}

	HashMap<String, JSONObject> getOrderDetails() {
		return orderDetails;
	}

	void setOrderDetails(HashMap<String, JSONObject> orderDetails) {
		this.orderDetails = orderDetails;
	}

	JSONObject getCustomerInfo() {
		return customerInfo;
	}

	void setCustomerInfo(JSONObject customerInfo) {
		this.customerInfo = customerInfo;
	}
	
	// this is a helper function, to convert the event_time to datetime, UTC
	static LocalDateTime convertDateTimeFromJSONObjectToUTC(JSONObject event)
	{
		Instant eventDateTime = Instant.parse((CharSequence) event.get("event_time"));
		// converted to UTC
		LocalDateTime eventDateTimeConvertedToUTC = LocalDateTime.ofInstant(eventDateTime,
				ZoneId.of(ZoneOffset.UTC.getId()));
		return eventDateTimeConvertedToUTC;
	}
	
}
