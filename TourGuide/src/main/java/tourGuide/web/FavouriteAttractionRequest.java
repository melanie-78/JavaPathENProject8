package tourGuide.web;

import gpsUtil.location.Location;

import java.util.List;

public class FavouriteAttractionRequest {
    private Location userLocation;
    private List<AttractionInfo> attractionInfos;

    public FavouriteAttractionRequest() {
    }

    public Location getUserLocation() {
        return userLocation;
    }

    public void setUserLocation(Location userLocation) {
        this.userLocation = userLocation;
    }

    public List<AttractionInfo> getAttractionInfos() {
        return attractionInfos;
    }

    public void setAttractionInfos(List<AttractionInfo> attractionInfos) {
        this.attractionInfos = attractionInfos;
    }
}
