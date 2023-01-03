package tourGuide;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.jsoniter.output.JsonStream;

import gpsUtil.location.VisitedLocation;
import tourGuide.service.RewardsService;
import tourGuide.service.TourGuideService;
import tourGuide.user.User;
import tourGuide.user.UserPreferences;
import tourGuide.web.FavouriteAttractionRequest;
import tourGuide.web.UserPreferencesRequest;
import tripPricer.Provider;

@RestController
public class TourGuideController {

	@Autowired
	TourGuideService tourGuideService;

    @Autowired
    RewardsService rewardsService;

    @RequestMapping("/")
    public String index() {
        return "Greetings from TourGuide!";
    }
    
    @RequestMapping("/getLocation") 
    public String getLocation(@RequestParam String userName) throws ExecutionException, InterruptedException {
    	VisitedLocation visitedLocation = tourGuideService.getUserLocation(getUser(userName));
		return JsonStream.serialize(visitedLocation.location);
    }


    @RequestMapping("/getNearbyAttractions") 
    public String getNearAttractions(@RequestParam String userName) throws ExecutionException, InterruptedException {
        FavouriteAttractionRequest nearAttractions = tourGuideService.getNearAttractions(userName);
        return JsonStream.serialize(nearAttractions);
    }

    /**
     * This method permits at a user to set their preferences

     * @param username is the name of an application's user
     * @param userPreferencesRequest an object which contains the different preferences : duration of trip,
     quantity of ticket, numbers of adults and numbers of children
     * @return a code response to specify if the request is ok or not
     */
    @PostMapping("/setUserPreferences")
    public ResponseEntity<?> setUserPreferences(@RequestParam String username,
                                                @RequestBody UserPreferencesRequest userPreferencesRequest) {

        UserPreferences userPreferences = new UserPreferences();

        userPreferences.setTripDuration(userPreferencesRequest.getTripDuration());
        userPreferences.setTicketQuantity(userPreferencesRequest.getTicketQuantity());
        userPreferences.setNumberOfAdults(userPreferencesRequest.getNumberOfAdults());
        userPreferences.setNumberOfChildren(userPreferencesRequest.getNumberOfChildren());

        User user = tourGuideService.getUser(username);
        user.setUserPreferences(userPreferences);

        return ResponseEntity.ok().build();
    }

    @RequestMapping("/getRewards") 
    public String getRewards(@RequestParam String userName) {
    	return JsonStream.serialize(tourGuideService.getUserRewards(getUser(userName)));
    }
    
    @RequestMapping("/getAllCurrentLocations")
    public String getAllCurrentLocations() {
    	// TODO: Get a list of every user's most recent location as JSON
    	//- Note: does not use gpsUtil to query for their current location, 
    	//        but rather gathers the user's current location from their stored location history.
    	//
    	// Return object should be the just a JSON mapping of userId to Locations similar to:
    	//     {
    	//        "019b04a9-067a-4c76-8817-ee75088c3822": {"longitude":-48.188821,"latitude":74.84371} 
    	//        ...
    	//     }

    	return JsonStream.serialize("");
    }
    
    @RequestMapping("/getTripDeals")
    public String getTripDeals(@RequestParam String userName) {
    	List<Provider> providers = tourGuideService.getTripDeals(getUser(userName));
    	return JsonStream.serialize(providers);
    }
    
    private User getUser(String userName) {
    	return tourGuideService.getUser(userName);
    }
   

}