import java.io.FileReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

class IngestEvents {

	static JSONParser parser = new JSONParser();
	// timeframe for LTV calculation
	static LocalDate start = null, end = null;

	/*
	 * ingest : path, HashMap<String, CustomerDetails> -> void The ingest takes
	 * 2 parameters, path to the file containing all the events and a hashmap
	 * that maps customerId's to their respective CustomerDetails object The
	 * ingest method populates the custIdToCustDetails map such that all the
	 * non-corrupted events are ingested
	 */
	// Since JSONObject (from JSON simple, derived from HashMap) does not use
	// generic parameters, the suppress warning is added
	@SuppressWarnings("unchecked")
	static void ingest(String path, HashMap<String, CustomerDetails> custIdToCustDetails) {
		try {
			JSONArray allCustEvents = (JSONArray) parser.parse(new FileReader(path));
			for (Object custEvents : allCustEvents) {
				JSONObject event = (JSONObject) custEvents;
				String type = (String) event.get("type");
				String customerId = (type != null && type.equals("CUSTOMER")) ? (String) event.get("key")
						: (String) event.get("customer_id");
				String verb = (String) event.get("verb");
				LocalDateTime eventDateTime = ((String) event.get("event_time") != null)? CustomerDetails.convertDateTimeFromJSONObjectToUTC(event) : null;
				String eventKey = (String) event.get("key");
				// only when the below cases are true, the event is processed
				// else I choose to ignore the event because the event is considered to be corrupted if it has missing/null fields
				if (type != null && !type.isEmpty() && customerId != null && !customerId.isEmpty()
						&& verb != null && !verb.isEmpty() && (verb.equals("NEW") || verb.equals("UPDATE") || verb.equals("UPLOAD")) 
						&& eventDateTime != null && eventKey != null && !eventKey.isEmpty()) {
					/* 
					 * The timeframe for the events, i.e. start and end date is checked here and not only in SITE_VISIT to handle 
					 * cases when there is a missing or corrupted SITE_VISIT but other events, either CUSTOMER or IMAGE or ORDER exist
					 */
					
					start = (start != null)? ((eventDateTime.toLocalDate().isBefore(start))? eventDateTime.toLocalDate() : start) : eventDateTime.toLocalDate();
					end = (end != null)? ((eventDateTime.toLocalDate().isAfter(end))? eventDateTime.toLocalDate() : end) : eventDateTime.toLocalDate();
					
					
					switch (type) {
					case "CUSTOMER":
						if (custIdToCustDetails.containsKey(customerId)) {
							/*
							 * if any other event has occurred before the
							 * CUSTOMER event that has caused the creation of
							 * CustomerDetails object then CustomerInfo will be
							 * null
							 */
							if (custIdToCustDetails.get(customerId).getCustomerInfo() != null
									&& verb.equals("UPDATE")) {
								/*
								 * 1. The verb is specifically checked for to be
								 * an update when getCustomerInfo != null
								 * because I choose to ignore the NEW verb in
								 * cases where the customer info is already
								 * present. This is the case that arises when an
								 * UPDATE event has been encountered before a
								 * NEW 
								 * 2. Considering the case where there can
								 * be more than 1 update the event_time is
								 * checked
								 */
								JSONObject storedEvent = custIdToCustDetails.get(customerId).getCustomerInfo();
								if (IsCurrentEventLatest(event, storedEvent))
									custIdToCustDetails.get(customerId).setCustomerInfo(event);
							} else
								custIdToCustDetails.get(customerId).setCustomerInfo(event);
						} else {
							CustomerDetails custDet = new CustomerDetails(customerId);
							custDet.setCustomerInfo(event);
							custIdToCustDetails.put(customerId, custDet);
						}
						break;
					case "SITE_VISIT":
						if (custIdToCustDetails.containsKey(customerId)) {
							if (custIdToCustDetails.get(customerId).getSiteVisits() != null) {
								/*
								 * since there is no "UPDATE" for site_visits,
								 * the below if case can be true only if there
								 * is duplication. hence I choose to ignore it.
								 */
								if (custIdToCustDetails.get(customerId).getSiteVisits().containsKey(eventKey))
									continue;
								else
									custIdToCustDetails.get(customerId).getSiteVisits().put(eventKey, event);
							} else {
								HashMap<String, JSONObject> siteVisitInit = new HashMap<String, JSONObject>();
								siteVisitInit.put(eventKey, event);
								custIdToCustDetails.get(customerId).setSiteVisits(siteVisitInit);
							}
						}
						// case when a SITE_VISIT event appears before a
						// "CUSTOMER" event
						else {
							CustomerDetails custDet = new CustomerDetails(customerId);
							HashMap<String, JSONObject> siteVisitInit = new HashMap<String, JSONObject>();
							siteVisitInit.put(eventKey, event);
							custDet.setSiteVisits(siteVisitInit);
							custIdToCustDetails.put(customerId, custDet);
						}
						break;
					case "IMAGE":
						if (custIdToCustDetails.containsKey(customerId)) {
							if (custIdToCustDetails.get(customerId).getImageUploadEvents() != null) {
								/*
								 * since there is no "UPDATE" for Image upload
								 * events, the below if case can be true only if
								 * there is duplication. hence I choose to
								 * ignore it.
								 */
								if (custIdToCustDetails.get(customerId).getImageUploadEvents()
										.containsKey(eventKey))
									continue;
								else
									custIdToCustDetails.get(customerId).getImageUploadEvents().put(eventKey,
											event);
							} else {
								HashMap<String, JSONObject> imageUploadInit = new HashMap<String, JSONObject>();
								imageUploadInit.put(eventKey, event);
								custIdToCustDetails.get(customerId).setImageUploadEvents(imageUploadInit);
							}
						}
						// case when an IMAGE upload event appears before a
						// CUSTOMER event
						else {
							CustomerDetails custDet = new CustomerDetails(customerId);
							HashMap<String, JSONObject> imageUploadInit = new HashMap<String, JSONObject>();
							imageUploadInit.put(eventKey, event);
							custDet.setImageUploadEvents(imageUploadInit);
							custIdToCustDetails.put(customerId, custDet);
						}
						break;
					case "ORDER":
						if (custIdToCustDetails.containsKey(customerId)) {
							if (custIdToCustDetails.get(customerId).getOrderDetails() != null) {
								/*
								 * 1. The verb is specifically checked for to be
								 * an UPDATE because I choose to ignore the NEW
								 * verb in cases where the order_id is already
								 * present. This is the case that arises when an
								 * UPDATE event has been encountered before a
								 * NEW 
								 * 2. Considering the case where there can
								 * be more than 1 update the event_time is
								 * checked
								 */
								if (custIdToCustDetails.get(customerId).getOrderDetails().containsKey(eventKey)
										&& verb.equals("UPDATE")) {
									JSONObject storedEvent = custIdToCustDetails.get(customerId).getOrderDetails()
											.get(eventKey);
									if (IsCurrentEventLatest(event, storedEvent))
										custIdToCustDetails.get(customerId).getOrderDetails().put(eventKey, event);
								} else
									custIdToCustDetails.get(customerId).getOrderDetails().put(eventKey, event);
							} else {
								HashMap<String, JSONObject> orderVisitInit = new HashMap<>();
								orderVisitInit.put(eventKey, event);
								custIdToCustDetails.get(customerId).setOrderDetails(orderVisitInit);
							}
						}
						// case when ORDER event appears before CUSTOMER event
						else {
							CustomerDetails custDet = new CustomerDetails(customerId);
							HashMap<String, JSONObject> orderVisitInit = new HashMap<String, JSONObject>();
							orderVisitInit.put(eventKey,event);
							custDet.setOrderDetails(orderVisitInit);
							custIdToCustDetails.put(customerId, custDet);
						}
						break;
					default:
						Logger.logMessage(String.format("Invalid event type encountered : %s", type));
						break;
					}
					custIdToCustDetails.get(customerId).getVisitedDatesAndTime().add(eventDateTime);
				}
			}
		} catch (Exception e) {
			// have hard coded the method name, other option is to use stack
			// trace or reflection to get method name
			Logger.logMessage(String.format("Exception in ingest method, Message : %s", e.getMessage()));
		}
	}

	static boolean IsCurrentEventLatest(JSONObject currEvent, JSONObject storedEvent) {
		return CustomerDetails.convertDateTimeFromJSONObjectToUTC(currEvent)
				.isAfter(CustomerDetails.convertDateTimeFromJSONObjectToUTC(storedEvent));
	}

}
