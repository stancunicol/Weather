package Common;

import java.io.Serializable;
import java.util.Objects;

public class WeatherInfo implements Serializable {
    private String date;
    private String condition;
    private double temperature;
    private double humidity;
    private double windSpeed;

    public WeatherInfo(String date, String condition, double temperature, double humidity, double windSpeed) {
        this.date = date;
        this.condition = condition;
        this.temperature = temperature;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getCondition() {
        return condition;
    }

    public double getTemperature() {
        return temperature;
    }

    public double getHumidity() {
        return humidity;
    }

    public double getWindSpeed() {
        return windSpeed;
    }

    @Override
    public String toString() {
        return "WeatherInfo{" +
                "date='" + date + '\'' +
                ", condition='" + condition + '\'' +
                ", temperature=" + temperature +
                ", humidity=" + humidity +
                ", windSpeed=" + windSpeed +
                '}';
    }
}