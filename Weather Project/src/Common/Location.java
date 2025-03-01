package Common;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Collections;

public class Location implements Serializable {
    private String name;
    private double latitude;
    private double longitude;
    private List<WeatherInfo> forecast;

    public Location(String name, double latitude, double longitude, List<WeatherInfo> forecast) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.forecast = forecast;
        //sort pe temp
    }

    public String getName() {
        return name;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public List<WeatherInfo> getForecast() {
        return forecast;
    }

    @Override
    public String toString() {
        return "Location{" +
                "name='" + name + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", forecast=" + forecast +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Location location = (Location) o;
        return Double.compare(latitude, location.latitude) == 0 && Double.compare(longitude, location.longitude) == 0 && Objects.equals(name, location.name) && Objects.equals(forecast, location.forecast);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, latitude, longitude, forecast);
    }

    public static void sortByTemperature(List<Location> locations) {
        locations.sort((loc1, loc2) -> {
            double avgTemp1 = loc1.getForecast() != null
                    ? loc1.getForecast().stream()
                    .mapToDouble(WeatherInfo::getTemperature)
                    .average()
                    .orElse(Double.NaN)
                    : Double.NaN;

            double avgTemp2 = loc2.getForecast() != null
                    ? loc2.getForecast().stream()
                    .mapToDouble(WeatherInfo::getTemperature)
                    .average()
                    .orElse(Double.NaN)
                    : Double.NaN;

            return Double.compare(avgTemp1, avgTemp2);
        });
    }
}
