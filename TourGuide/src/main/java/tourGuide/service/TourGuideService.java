package tourGuide.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import tourGuide.helper.InternalTestHelper;
import tourGuide.tracker.Tracker;
import tourGuide.user.User;
import tourGuide.user.UserReward;
import tourGuide.web.AttractionInfo;
import tourGuide.web.AttractionNearUser;
import tourGuide.web.FavouriteAttractionRequest;
import tripPricer.Provider;
import tripPricer.TripPricer;

@Service
public class TourGuideService {
	private Logger logger = LoggerFactory.getLogger(TourGuideService.class);
	private final GpsUtil gpsUtil;
	private final RewardsService rewardsService;
	private final TripPricer tripPricer = new TripPricer();
	public final Tracker tracker;
	boolean testMode = true;
	
	public TourGuideService(GpsUtil gpsUtil, RewardsService rewardsService) {
		this.gpsUtil = gpsUtil;
		this.rewardsService = rewardsService;
		
		if(testMode) {
			logger.info("TestMode enabled");
			logger.debug("Initializing users");
			initializeInternalUsers();
			logger.debug("Finished initializing users");
		}
		tracker = new Tracker(this);
		addShutDownHook();
	}
	
	public List<UserReward> getUserRewards(User user) {
		return user.getUserRewards();
	}

	public VisitedLocation getUserLocation(User user) {
		VisitedLocation visitedLocation = (user.getVisitedLocations().size() > 0) ?
			user.getLastVisitedLocation() :
			trackUserLocation(user);
		return visitedLocation;
	}
	
	public User getUser(String userName) {
		return internalUserMap.get(userName);
	}
	
	public List<User> getAllUsers() {
		return internalUserMap.values().stream().collect(Collectors.toList());
	}
	
	public void addUser(User user) {
		if(!internalUserMap.containsKey(user.getUserName())) {
			internalUserMap.put(user.getUserName(), user);
		}
	}
	
	public List<Provider> getTripDeals(User user) {
		int cumulatativeRewardPoints = user.getUserRewards().stream().mapToInt(i -> i.getRewardPoints()).sum();
		List<Provider> providers = tripPricer.getPrice(tripPricerApiKey, user.getUserId(), user.getUserPreferences().getNumberOfAdults(), 
				user.getUserPreferences().getNumberOfChildren(), user.getUserPreferences().getTripDuration(), cumulatativeRewardPoints);
		user.setTripDeals(providers);
		return providers;
	}


	public VisitedLocation trackUserLocation(User user) {
		VisitedLocation visitedLocation = gpsUtil.getUserLocation(user.getUserId());
		user.addToVisitedLocations(visitedLocation);
		rewardsService.calculateRewards(user);
		return visitedLocation;
	}

	/**
	 * @return this method returns a list of tourists attractions
	 */
	public List<Attraction> getTouristsAttractions(){
		List<Attraction> attractions = gpsUtil.getAttractions();
		return attractions;
	}

	/**
	 * @param userName is the name of an application's user
	 * @return an object type which contains the name of Tourist attraction, the tourist attractions lat/long, the user's location lat/long,
	 the distance in miles between the user's location and each of the attractions ant the reward points for visiting each Attraction.
	 */

	public FavouriteAttractionRequest getNearAttractions(String userName){
		FavouriteAttractionRequest favouriteAttractionRequest = new FavouriteAttractionRequest();

		User user = getUser(userName);
		VisitedLocation visitedLocation= getUserLocation(user);

		// return the user's location
		Location userLocation = visitedLocation.location;

		// return distance in miles between the user's location and each of the attractions.
		// calculate the distance
		List<AttractionNearUser> listOfAttractionNearUser = getTouristsAttractions().stream().map(attraction -> {
					AttractionNearUser attractionNearUser = new AttractionNearUser();

					attractionNearUser.setAttraction(attraction);
					double distance = rewardsService.getDistance(attraction, userLocation);
					attractionNearUser.setDistanceNearUser(distance);

					return  attractionNearUser;
				}).sorted((o1, o2)-> (int) (o1.getDistanceNearUser() - o2.getDistanceNearUser()))
				.limit(5)
				.collect(Collectors.toList());

		// return The reward points for visiting each Attraction
		listOfAttractionNearUser.forEach(attractionNearUser -> {
			int rewardPoints = rewardsService.getRewardPoints(attractionNearUser.getAttraction(), user);
			attractionNearUser.setRewardPoint(rewardPoints);
		});

		List<AttractionInfo> attractionInfoList = listOfAttractionNearUser.stream().map(attractionNearUser -> {
			AttractionInfo attractionInfo = new AttractionInfo();

			attractionInfo.setAttractionName(attractionNearUser.getAttraction().attractionName);
			attractionInfo.setAttractionLocation(new Location(attractionNearUser.getAttraction().latitude, attractionNearUser.getAttraction().longitude));
			attractionInfo.setDistance(attractionNearUser.getDistanceNearUser());
			attractionInfo.setRewardPoint(attractionNearUser.getRewardPoint());

			return attractionInfo;
		}).collect(Collectors.toList());

		favouriteAttractionRequest.setUserLocation(userLocation);
		favouriteAttractionRequest.setAttractionInfos(attractionInfoList);

		return favouriteAttractionRequest;
	}

	public List<Attraction> getNearByAttractions(VisitedLocation visitedLocation) {
		List<Attraction> nearbyAttractions = new ArrayList<>();
		for(Attraction attraction : gpsUtil.getAttractions()) {
			if(rewardsService.isWithinAttractionProximity(attraction, visitedLocation.location)) {
				nearbyAttractions.add(attraction);
			}
		}
		
		return nearbyAttractions;
	}
	
	private void addShutDownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() { 
		      public void run() {
		        tracker.stopTracking();
		      } 
		    }); 
	}
	
	/**********************************************************************************
	 * 
	 * Methods Below: For Internal Testing
	 * 
	 **********************************************************************************/
	private static final String tripPricerApiKey = "test-server-api-key";
	// Database connection will be used for external users, but for testing purposes internal users are provided and stored in memory
	private final Map<String, User> internalUserMap = new HashMap<>();
	private void initializeInternalUsers() {
		IntStream.range(0, InternalTestHelper.getInternalUserNumber()).forEach(i -> {
			String userName = "internalUser" + i;
			String phone = "000";
			String email = userName + "@tourGuide.com";
			User user = new User(UUID.randomUUID(), userName, phone, email);
			generateUserLocationHistory(user);
			
			internalUserMap.put(userName, user);
		});
		logger.debug("Created " + InternalTestHelper.getInternalUserNumber() + " internal test users.");
	}
	
	private void generateUserLocationHistory(User user) {
		IntStream.range(0, 3).forEach(i-> {
			user.addToVisitedLocations(new VisitedLocation(user.getUserId(), new Location(generateRandomLatitude(), generateRandomLongitude()), getRandomTime()));
		});
	}
	
	private double generateRandomLongitude() {
		double leftLimit = -180;
	    double rightLimit = 180;
	    return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}
	
	private double generateRandomLatitude() {
		double leftLimit = -85.05112878;
	    double rightLimit = 85.05112878;
	    return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}
	
	private Date getRandomTime() {
		LocalDateTime localDateTime = LocalDateTime.now().minusDays(new Random().nextInt(30));
	    return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
	}
	
}
