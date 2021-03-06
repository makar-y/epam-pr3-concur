/*
 * Makary Malinouski
 * 
 * This is project 3 for Epam Java web-development course. 
 * The theme of the project is Concurrency.
 * The task is to simulate Uber.
 */
package by.malinouski.uber.timedistance;

import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import by.malinouski.uber.client.Client;
import by.malinouski.uber.exception.EmptyTaxiSetException;
import by.malinouski.uber.location.Location;
import by.malinouski.uber.taxi.Taxi;

/**
 * @author makarymalinouski
 * This class' purpose is Distance calculation
 */
public class Calculator {

    final static Logger LOGGER = LogManager.getLogger();
    private static double SPEED = 1000.0; // meters/min 
    private static double EARTH_RADIUS = 6371e3;
    
    /*
     * !! Returns null if taxis is empty
     */
    public BestValue calcBestValue(Set<Taxi> taxis, Client client) 
                                        throws EmptyTaxiSetException {
        LOGGER.debug("TAXIS EMPTY: " + taxis.isEmpty());
        if (taxis.isEmpty()) {
            throw new EmptyTaxiSetException();
        }
        LOGGER.debug("In calcBestValue");
        TimeDistance bestTimeDist = null;
        TimeDistance timeDist = null;
        Taxi bestTaxi = null;
        
        for (Taxi taxi : taxis) { 
            if (taxi.getTaxiState().isAvailable()
                && (bestTimeDist == null 
                    | (timeDist = calcTimeDistance(
                            taxi.getLocation(), client.getLocation()))
                        .compareTo(bestTimeDist) < 0)) {
                
                bestTimeDist = timeDist; 
                bestTaxi = taxi;
            }
        }
        
        /* OPTIMIZATION: 
         * if no taxi is available, which shouldn't happen too often
         * (hence it's in a separate loop. Another version could be
         * to bring this check in the main loop, but it would slow down
         * the main process)
         * see which one is best considering the timeDistance it will take 
         * to bring current client to location
         */
        if (bestTaxi == null) {
            LOGGER.debug("In if(bestTaxi == null)");
            for (Taxi taxi : taxis) {
                timeDist = addTimeDistances(
                        taxi.getTotalTimeDistance(),
                        calcTimeDistance(taxi.getFinalTargetLocation(), client.getLocation()));
                
                LOGGER.debug(String.format("CHOOSING: taxi %d, %s", taxi.getTaxiId(), timeDist));
                if (bestTimeDist == null || timeDist.compareTo(bestTimeDist) < 0) {
                    bestTimeDist = timeDist;
                    bestTaxi = taxi;
                }
            }
            LOGGER.debug("IN calcBestValue: CHOSE " + bestTimeDist);
        }
        
        // this is needed for the optimization above
        bestTaxi.setTotalTimeDistance(
                addTimeDistances(bestTimeDist, 
                                calcTimeDistance(client.getLocation(), 
                                        client.getTargetLocation())));
        
        bestTaxi.setFinalTargetLocation(client.getTargetLocation());
        
        return new BestValue(bestTaxi, bestTimeDist);
    }
    
    /**
     * Calculates distance between two locations
     */
    public TimeDistance calcTimeDistance(Location loc1, Location loc2) {
        
        double distance = calcDistance(
                loc1.getLatitude(), loc1.getLongitude(),
                loc2.getLatitude(), loc2.getLongitude());
        
        LOGGER.debug("In CalcTimeDistance: distance - " + distance);
        return new TimeDistance((int) distance, SPEED);
    }
    
    public TimeDistance addTimeDistances(TimeDistance td1, TimeDistance td2) {
        int meters = td1.getMeters() + td2.getMeters();
        int minutes = td1.getMinutes() + td2.getMinutes();
        
        return new TimeDistance(meters, minutes);
    }
    
    
    public int calcArrivalTime(Taxi taxi, Client client) {
        /* if the taxi was chosen first time calc time between
         * current loc and client's one,
         * if not take to the account other client's time 
         */
        return taxi.getTaxiState().isAvailable()                            
                ? calcTimeDistance(taxi.getLocation(), client.getLocation())
                  .getMinutes()
                : addTimeDistances(taxi.getTotalTimeDistance(),
                        calcTimeDistance(taxi.getFinalTargetLocation(), client.getLocation()))
                  .getMinutes();
    }
    
    // http://www.movable-type.co.uk/scripts/latlong.html
    private double calcDistance(double lat1, double lon1, double lat2, double lon2) {
         double lat1Radian = Math.toRadians(lat1);
         double lat2Radian = Math.toRadians(lat2);
         double latDiff = Math.toRadians(lat2 - lat1);
         double longDiff = Math.toRadians(lon2 - lon1);
         
         double a = Math.pow(Math.sin(latDiff/2), 2) +
                    Math.cos(lat1Radian) * Math.cos(lat2Radian) *
                    Math.pow(Math.sin(longDiff/2), 2);
         
         double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
         
         double distance = EARTH_RADIUS * c;
         
         return distance;
         
    }
}
