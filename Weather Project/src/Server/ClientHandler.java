package Server;

import Common.Location;

import Common.WeatherInfo;

import java.io.*;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler implements Runnable {
    private Socket socket;
    private Connection connection;
    private List<Location> locationsDatabase;

    public ClientHandler(Socket socket, Connection connection, List<Location> locationsDatabase) {
        this.socket = socket;
        this.connection = connection;
        this.locationsDatabase = locationsDatabase;
    }

    @Override
    public void run() {
        try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

            String request = (String) in.readObject();

            if (request.startsWith("COORDINATES")) {
                String[] parts = request.split(",");
                double latitude = Double.parseDouble(parts[1]);
                double longitude = Double.parseDouble(parts[2]);

                System.out.println("Received coordinates: Latitude=" + latitude + ", Longitude=" + longitude);
                Location closestLocation = findClosestLocation(latitude, longitude);
                System.out.println("Closest location found: " + closestLocation);

                out.writeObject(closestLocation);
            } else {
                List<WeatherInfo> forecasts = getForecastsForLocation(request, out);

                if (forecasts.isEmpty()) {
                    out.writeObject(null);
                } else {
                    Location location = new Location(request, 0, 0, forecasts);
                    out.writeObject(location);
                }
            }
            out.flush();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error handling client request: " + e.getMessage());
        }
    }


    private List<WeatherInfo> getForecastsForLocation(String locationName, ObjectOutputStream out) {
        List<WeatherInfo> forecasts = new ArrayList<>();
        String query = """
        SELECT wi.forecast_date AS date, wi.condition, wi.temperature, wi.humidity, wi.wind_speed
        FROM weather_info wi
        JOIN locations l ON wi.location_id = l.id
        WHERE LOWER(l.name) = LOWER(?)
    """;
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, locationName.toLowerCase());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    forecasts.add(new WeatherInfo(
                            rs.getString("date"),
                            rs.getString("condition"),
                            rs.getDouble("temperature"),
                            rs.getDouble("humidity"),
                            rs.getDouble("wind_speed")));
                }
                System.out.println("Fetching forecast for location: " + locationName);
                if (forecasts.isEmpty()) {
                    System.out.println("No data found for location: " + locationName);
                    try {
                        out.writeObject(null);
                    } catch (IOException e) {
                        System.err.println("Error writing to output stream: " + e.getMessage());
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error querying weather_info: " + e.getMessage());
        }
        return forecasts;
    }

    private Location findClosestLocation(double latitude, double longitude) {
        Location closestLocation = null;
        double smallestDifference = Double.MAX_VALUE;

        System.out.println(locationsDatabase.size());

        for (Location location : locationsDatabase) {
            double latDifference = Math.abs(location.getLatitude() - latitude);
            double lonDifference = Math.abs(location.getLongitude() - longitude);

            double difference = latDifference + lonDifference;

            System.out.println("Checking location: " + location.getName() +
                    " (Lat: " + location.getLatitude() + ", Lon: " + location.getLongitude() +
                    "), Difference: " + difference);
            if (difference < smallestDifference) {
                smallestDifference = difference;
                closestLocation = location;
            }
        }

        System.out.println("Closest location found: " + (closestLocation != null ? closestLocation.getName() : "None"));
        return closestLocation;
    }
}