package com.comp30231.coursework;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class MainController {

	private static String userId = "";

	public JSONObject runApiCall(URI uri, Boolean isWeather) {
		JSONObject result = new JSONObject();
		try (InputStream input = uri.toURL().openStream()) {
			InputStreamReader isr = new InputStreamReader(input);
			BufferedReader reader = new BufferedReader(isr);
			StringBuilder json = new StringBuilder();
			int c;
			while ((c = reader.read()) != -1) {
				json.append((char) c);
			}
			if (isWeather == true) {
				result = new JSONObject(json.toString());  
			} else {
				result = new JSONObject();
				result.put("result", json.toString().replace("\n", ""));
			}

		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return result;
	}

	public JSONObject readFile(String fileName) {
		JSONObject result = new JSONObject();
		try {
			FileReader fileReader = new FileReader(fileName);
			BufferedReader reader = new BufferedReader(fileReader);
			StringBuilder json = new StringBuilder();
			int c;
			while ((c = reader.read()) != -1) {
				json.append((char) c);
			}
			reader.close();
			if (json.length() != 0) {
				result = new JSONObject(json.toString());  
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return result;
	}

	public Boolean writeToFile(String fileName, JSONObject jsonObject) {
		Boolean writeSuccess = false;
		try {
         FileWriter file = new FileWriter(fileName);
         file.write(jsonObject.toString(2));
         file.close();
		 writeSuccess = true;
      } catch (IOException ioe) {
         // TODO Auto-generated catch block
         ioe.printStackTrace();
      }
	  return writeSuccess;
	}

	@RequestMapping(value = "register", method = RequestMethod.GET)
	public String register(Model model) {
		User user = new User();
        model.addAttribute("user", user);
		return "register";
	}

	@RequestMapping(value = "submitRegister", method = RequestMethod.POST)
	public String submitRegister(@ModelAttribute("user") User user, RedirectAttributes attributes) throws JSONException, URISyntaxException {
		User userNew = new User();
        attributes.addFlashAttribute("user", userNew);
		attributes.addFlashAttribute("result", "Username already exists, please try another one.");
		String username = user.getUsername();
		String password = user.getPassword();
		JSONObject allUsers = readFile("./src/main/resources/allUsers.json");
		JSONObject userObject = new JSONObject();
		userObject.put("userID", runApiCall(new URI("https://www.random.org/strings/?num=1&len=10&digits=on&unique=on&format=plain&rnd=new"), false).getString("result"));
		userObject.put("username", username);
		userObject.put("password", password);
		if (allUsers.isEmpty()) {
			JSONObject root = new JSONObject();
			JSONArray users = new JSONArray();
			users.put(userObject);
			root.put("users", users);
			if (writeToFile("./src/main/resources/allUsers.json", root) == true) {
				attributes.addFlashAttribute("result", "User created successfully.");
			}
		} else {
			JSONArray users = allUsers.getJSONArray("users");
			for (int i = 0; i < users.length(); i++) {
				JSONObject userObj = users.getJSONObject(i);
				if (userObj.getString("username").equalsIgnoreCase(username)) {
					attributes.addFlashAttribute("result", "Username already exists, please try another one.");
					return "redirect:/register";
				}
			}
			users.put(userObject);
			allUsers.put("users", users);
			if (writeToFile("./src/main/resources/allUsers.json", allUsers) == true) {
				attributes.addFlashAttribute("result", "User created successfully.");
			}
		}
		return "redirect:/login";
	}

	@RequestMapping(value = "login", method = RequestMethod.GET)
	public String login(Model model) {
		User user = new User();
        model.addAttribute("user", user);
		return "login";
	}

	@RequestMapping(value = "submitLogin", method = RequestMethod.POST)
	public String submitLogin(@ModelAttribute("user") User user, RedirectAttributes attributes) throws JSONException, URISyntaxException {
		User userNew = new User();
        attributes.addFlashAttribute("user", userNew);
		Boolean found = false;
		String username = user.getUsername();
		String password = user.getPassword();
		JSONObject allUsers = readFile("./src/main/resources/allUsers.json");
		if (allUsers.isEmpty()) {
			attributes.addFlashAttribute("result", "User not found on system.");
			return "redirect:/login";
		} else {
			JSONArray allUsersArray = allUsers.getJSONArray("users");
			for (int i = 0; i < allUsersArray.length(); i++) {
				JSONObject userObject = allUsersArray.getJSONObject(i);
				if (userObject.getString("username").equalsIgnoreCase(username)
				&& userObject.getString("password").equalsIgnoreCase(password)) {
					userId = userObject.getString("userID");
					found = true;
				}
			}
		}
		if (found == false) {
			attributes.addFlashAttribute("result", "Incorrect login details, please try again.");
			return "redirect:/login";
		} else {
			attributes.addFlashAttribute("result", "");
			return "redirect:/queryForTrip";
		}
	}

	@RequestMapping(value = "logout", method = RequestMethod.GET)
	public String logout(RedirectAttributes attributes) {
		userId = "";
		attributes.addFlashAttribute("result", "Logged out successfully.");
		return "redirect:/login";
	}

	@RequestMapping(value = "queryForTrip", method = RequestMethod.GET)
	public String queryForTrip(Model model) {
		if (userId.equalsIgnoreCase("")) {
			return "redirect:/login";
		}
		TripQuery tripQuery = new TripQuery();
        model.addAttribute("tripQuery", tripQuery);
		return "queryForTrip";
	}

	@RequestMapping(value = "searchTrip", method = RequestMethod.POST)
	public String searchTrip(@ModelAttribute("tripQuery") TripQuery tripQuery, RedirectAttributes attributes) {
		TripQuery tripQueryNew = new TripQuery();
		JSONObject result = new JSONObject();
		JSONArray resultArray = new JSONArray();
		JSONObject allTrips = readFile("./src/main/resources/allTrips.json");
		if (!allTrips.isEmpty()) {
			JSONArray allTripsArray = allTrips.getJSONArray("trips");
			String location = tripQuery.getSearchText();
			String startDate = tripQuery.getStartDate();
			String endDate = tripQuery.getEndDate();
			for (int i = 0; i < allTripsArray.length(); i++) {
				JSONObject trip = allTripsArray.getJSONObject(i);
				if (startDate == null || startDate.equalsIgnoreCase("")) {
					if (trip.getString("location").equalsIgnoreCase(location)) {
						resultArray.put(trip);
					}				
				} else if (startDate != null && (endDate == null || endDate.equalsIgnoreCase(""))) {
					if (trip.getString("location").equalsIgnoreCase(location)
					&& trip.getString("startDate").equalsIgnoreCase(startDate)) {
						resultArray.put(trip);
					}
				} else if (startDate != null && endDate != null) {
					if (trip.getString("location").equalsIgnoreCase(location)
					&& trip.getString("startDate").equalsIgnoreCase(startDate)
					&& trip.getString("endDate").equalsIgnoreCase(endDate)) {
						resultArray.put(trip);
					}
				}
			}
		}
		result.put("trips", resultArray);
		attributes.addFlashAttribute("result", result.toString(2));
        attributes.addFlashAttribute("tripQuery", tripQueryNew);
		attributes.addFlashAttribute("searchValue", tripQuery.getSearchText());
		attributes.addFlashAttribute("startDateValue", tripQuery.getStartDate());
		attributes.addFlashAttribute("endDateValue", tripQuery.getEndDate());
		return "redirect:/queryForTrip";
	}

	@RequestMapping(value = "proposeNewTrip", method = RequestMethod.GET)
	public String proposeNewTrip(Model model) {
		if (userId.equalsIgnoreCase("")) {
			return "redirect:/login";		
		}
		TripProposal tripProposal = new TripProposal();
        model.addAttribute("tripProposal", tripProposal);
		return "proposeNewTrip";
	}

	@RequestMapping(value = "saveTrip", method = RequestMethod.POST)
	public String saveTrip(@ModelAttribute("tripProposal") TripProposal tripProposal, RedirectAttributes attributes) throws IOException, URISyntaxException
	{
		TripProposal tripProposalNew = new TripProposal();
        attributes.addFlashAttribute("tripProposal", tripProposalNew);
		attributes.addFlashAttribute("result", "Sorry, proposed trip could not be saved, please try again later.");
		String tripId = runApiCall(new URI("https://www.random.org/strings/?num=1&len=10&digits=on&loweralpha=on&unique=on&format=plain&rnd=new"), false).getString("result");
		String location = tripProposal.getLocation();
		String startDate = tripProposal.getStartDate();
		String endDate = tripProposal.getEndDate();
		URI weatherApiUri = new URI("http://api.weatherapi.com/v1/history.json?key=787e65fd92b94410944155232232108&q=" + location + "&dt=" + startDate + "&end_dt=" + endDate);
		JSONObject weatherApiResult = runApiCall(weatherApiUri, true);
		JSONObject allTrips = readFile("./src/main/resources/allTrips.json");
		JSONObject trip = new JSONObject();
		if (weatherApiResult.has("error") == true) {
			String message = weatherApiResult.getJSONObject("error").getString("message");
			attributes.addFlashAttribute("result", message);
		} else {
			//Inserting key-value pairs into the json object
			trip.put("userID", userId);
			trip.put("tripID", tripId);
			trip.put("location", location);
			trip.put("startDate", startDate);
			trip.put("endDate", endDate);
			if (weatherApiResult.has("forecast")) {
				if (weatherApiResult.getJSONObject("forecast").has("forecastday")) {
					trip.put("weather", weatherApiResult.getJSONObject("forecast").getJSONArray("forecastday"));
					if (allTrips.isEmpty()) {
						JSONObject root = new JSONObject();
						JSONArray trips = new JSONArray();
						trips.put(trip);
						root.put("trips", trips);
						if (writeToFile("./src/main/resources/allTrips.json", root) == true) {
							attributes.addFlashAttribute("result", "Proposed trip saved successfully.");
						}
					} else {
						JSONArray trips = allTrips.getJSONArray("trips");
						trips.put(trip);
						allTrips.put("trips", trips);
						if (writeToFile("./src/main/resources/allTrips.json", allTrips) == true) {
							attributes.addFlashAttribute("result", "Proposed trip saved successfully.");
						}
					}
				}
			} else {
				attributes.addFlashAttribute("result", "Sorry, proposed trip could not be saved, end date should be greater than start date and difference should not be more than 30 days between the two dates.");
			}
		}
		return "redirect:/proposeNewTrip";
	}

	public List<List<String>> getTripDropDownOptions(Boolean userRelated) {
		List<List<String>> options = new ArrayList<List<String>>();
		JSONObject allTrips = readFile("./src/main/resources/allTrips.json");
		if (!allTrips.isEmpty()) {
			JSONArray allTripsArray = allTrips.getJSONArray("trips");
			for (int i = 0; i < allTripsArray.length(); i++) {
				List<String> option = new ArrayList<String>();
				JSONObject trip = allTripsArray.getJSONObject(i);
				if (userRelated == false) {
					option.add(trip.getString("tripID"));
					option.add(trip.getString("location"));
					option.add(trip.getString("startDate"));
					option.add(trip.getString("endDate"));
				} else {
					if (userId.equalsIgnoreCase(trip.getString("userID"))) {
						option.add(trip.getString("tripID"));
						option.add(trip.getString("location"));
						option.add(trip.getString("startDate"));
						option.add(trip.getString("endDate"));
					}
				}
				if (!option.isEmpty()) {
					options.add(option);
				}
			}
		}
		return options;
	}

	@RequestMapping(value = "expressInterestInTrip", method = RequestMethod.GET)
	public String expressInterestInTrip(Model model) {
		if (userId.equalsIgnoreCase("")) {
			return "redirect:/login";		
		}
		InterestInTrip interestInTrip = new InterestInTrip();
		List<List<String>> options = getTripDropDownOptions(false);
        model.addAttribute("interestInTrip", interestInTrip);
		model.addAttribute("options", options);
		return "expressInterestInTrip";
	}

	@RequestMapping(value = "submitExpressInterestInTrip", method = RequestMethod.POST)
	public String submitExpressInterestInTrip(@ModelAttribute("interestInTrip") InterestInTrip interestInTrip, RedirectAttributes attributes) throws JSONException, URISyntaxException {
		InterestInTrip interestInTripNew = new InterestInTrip();
        attributes.addFlashAttribute("interestInTrip", interestInTripNew);
		attributes.addFlashAttribute("result", "Sorry, interest could not be expressed, please try again later.");
		String tripId = interestInTrip.getTripId();
		JSONObject allInterests = readFile("./src/main/resources/allTripInterests.json");
		JSONObject interest = new JSONObject();
		interest.put("userID", userId);
		interest.put("tripID", tripId);
		if (allInterests.isEmpty()) {
			JSONObject root = new JSONObject();
			JSONArray interests = new JSONArray();
			interests.put(interest);
			root.put("interests", interests);
			if (writeToFile("./src/main/resources/allTripInterests.json", root) == true) {
				attributes.addFlashAttribute("result", "Interest expressed successfully.");
			}
		} else {
			JSONArray interests = allInterests.getJSONArray("interests");
			interests.put(interest);
			allInterests.put("interests", interests);
			if (writeToFile("./src/main/resources/allTripInterests.json", allInterests) == true) {
				attributes.addFlashAttribute("result", "Interest expressed successfully.");
			}
		}
		return "redirect:/expressInterestInTrip";
	}

	@RequestMapping(value = "checkInterestInTrip", method = RequestMethod.GET)
	public String checkInterestInTrip(Model model) {
		if (userId.equalsIgnoreCase("")) {
			return "redirect:/login";	
		}
		InterestInTrip interestInTrip = new InterestInTrip();
		List<List<String>> options = getTripDropDownOptions(true);
        model.addAttribute("interestInTrip", interestInTrip);
		model.addAttribute("options", options);
		return "checkInterestInTrip";
	}

	@RequestMapping(value = "submitCheckInterestInTrip", method = RequestMethod.POST)
	public String submitCheckInterestInTrip(@ModelAttribute("interestInTrip") InterestInTrip interestInTrip, RedirectAttributes attributes) throws JSONException, URISyntaxException {
		InterestInTrip interestInTripNew = new InterestInTrip();
		JSONObject result = new JSONObject();
		JSONArray resultArray = new JSONArray();
		JSONObject allTripInterests = readFile("./src/main/resources/allTripInterests.json");
		if (userId.equalsIgnoreCase("")) {
			userId = runApiCall(new URI("https://www.random.org/strings/?num=1&len=10&digits=on&unique=on&format=plain&rnd=new"), false).getString("result");
		}
		if (!allTripInterests.isEmpty()) {
			JSONArray allTripInterestsArray = allTripInterests.getJSONArray("interests");
			String tripId = interestInTrip.getTripId();
			for (int i = 0; i < allTripInterestsArray.length(); i++) {
				JSONObject interest = allTripInterestsArray.getJSONObject(i);
				if (interest.getString("tripID").equalsIgnoreCase(tripId)) {
					resultArray.put(interest);
				}
			}
		}
		result.put("interests", resultArray);
		attributes.addFlashAttribute("result", result.toString(2));
		attributes.addFlashAttribute("interestInTrip", interestInTripNew);
		return "redirect:/checkInterestInTrip";
	}
}