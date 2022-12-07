package tourGuide.web;

import gpsUtil.location.Attraction;

public class AttractionNearUser {
    private Attraction attraction;
    private Double distanceNearUser;
    private int rewardPoint;

    public Attraction getAttraction() {
        return attraction;
    }

    public void setAttraction(Attraction attraction) {
        this.attraction = attraction;
    }

    public Double getDistanceNearUser() {
        return distanceNearUser;
    }

    public void setDistanceNearUser(Double distanceNearUser) {
        this.distanceNearUser = distanceNearUser;
    }

    public int getRewardPoint() {
        return rewardPoint;
    }

    public void setRewardPoint(int rewardPoint) {
        this.rewardPoint = rewardPoint;
    }
}
