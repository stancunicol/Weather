//când serverul se închide, deconectează serverul de baza de date
package Server;

import Common.Location;

import Common.WeatherInfo;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.scene.layout.VBox;

import javafx.application.Platform;

import java.io.*;
import java.lang.reflect.Type;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import Client.ClientInterface;

public class Server extends Application {
    private static final int PORT = 12345;
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/weather_db?charSet=UTF-8";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "1q2w3e";
    private Connection connection;
    private List<Location> locationsDatabase = new ArrayList<>();

    public Server() {
        try {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("Connected to the database successfully.");
        } catch (SQLException e) {
            System.err.println("Error connecting to the database: " + e.getMessage());
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public List<Location> getLocationsDatabase() {
        return locationsDatabase;
    }

    public void loadDataFromJson(String filePath) {
        if (!locationsDatabase.isEmpty()) {
            System.out.println("Data already loaded from JSON.");
            return;
        }

        try (Reader reader = new FileReader(filePath)) {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<Location>>() {}.getType();
            locationsDatabase = gson.fromJson(reader, listType);

            for (Location location : locationsDatabase) {
                for (WeatherInfo forecast : location.getForecast()) {
                    String forecastDate = forecast.getDate();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    try {
                        java.util.Date parsedDate = sdf.parse(forecastDate);
                        java.sql.Date sqlDate = new java.sql.Date(parsedDate.getTime());

                        String formattedDate = sdf.format(sqlDate);

                        forecast.setDate(formattedDate);
                    } catch (ParseException e) {
                        System.err.println("Error parsing date: " + forecastDate);
                    }
                }
            }

            Location.sortByTemperature(locationsDatabase);

            for (Location location : locationsDatabase) {
                System.out.println(location);
                System.out.println(location.getForecast().get(0).getTemperature());
            }

            System.out.println("Data loaded successfully from JSON.");
        } catch (IOException e) {
            System.err.println("Error loading data from JSON: " + e.getMessage());
        }
    }

    public void saveDataToDatabase() {
        if (locationsDatabase.isEmpty()) {
            System.out.println("No data to save to the database.");
            return;
        }

        try {
            String locationQuery = "INSERT INTO locations (name, latitude, longitude) VALUES (?, ?, ?) RETURNING id";
            PreparedStatement locationStmt = connection.prepareStatement(locationQuery);

            for (Location location : locationsDatabase) {
                if (!isLocationInDatabase(location.getName())) {
                    locationStmt.setString(1, location.getName());
                    locationStmt.setDouble(2, location.getLatitude());
                    locationStmt.setDouble(3, location.getLongitude());
                    ResultSet rs = locationStmt.executeQuery();
                    rs.next();
                    int locationId = rs.getInt(1);

                    String forecastQuery = "INSERT INTO weather_info (location_id, forecast_date, condition, temperature, humidity, wind_speed) VALUES (?, ?, ?, ?, ?, ?)";
                    PreparedStatement forecastStmt = connection.prepareStatement(forecastQuery);

                    for (WeatherInfo forecast : location.getForecast()) {
                        forecastStmt.setInt(1, locationId);
                        forecastStmt.setDate(2, Date.valueOf(forecast.getDate())); // Convertim String în Date
                        forecastStmt.setString(3, forecast.getCondition());
                        forecastStmt.setDouble(4, forecast.getTemperature());
                        forecastStmt.setDouble(5, forecast.getHumidity());
                        forecastStmt.setDouble(6, forecast.getWindSpeed());
                        forecastStmt.executeUpdate();
                    }
                } else {
                    System.out.println("Location " + location.getName() + " already exists in the database.");
                }
            }

            System.out.println("Data saved successfully to the database.");
        } catch (SQLException e) {
            System.err.println("Error saving data to database: " + e.getMessage());
        }
    }

    public boolean isLocationInDatabase(String locationName) {
        String query = "SELECT COUNT(*) FROM locations WHERE LOWER(name) = LOWER(?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, locationName.toLowerCase());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking database for location: " + e.getMessage());
        }
        return false;
    }

    public void start(Stage primaryStage) throws Exception {
        Label label = new Label("Choose your type of user:");

        Button btnAdmin = new Button("Admin");
        Button btnClient = new Button("Client");

        btnAdmin.setOnAction(event -> {
            System.out.println("Admin was selected.");
            openAdminInterface();
        });

        btnClient.setOnAction(event -> {
            System.out.println("Client was selected.");
            openClientInterface();
        });

        VBox vbox = new VBox(10);
        vbox.getChildren().addAll(label, btnAdmin, btnClient);

        vbox.setAlignment(javafx.geometry.Pos.CENTER);

        StackPane root = new StackPane();
        root.getChildren().add(vbox);

        Scene scene = new Scene(root, 300, 250);
        primaryStage.setTitle("");
        primaryStage.setScene(scene);

        primaryStage.show();

        startServer();
    }

    public void startServer() {
        Thread serverThread = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                loadLocationsFromDatabase();
                System.out.println("Server is running on port " + PORT + ".");

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("New client connection established.");
                    new Thread(new ClientHandler(clientSocket, connection, locationsDatabase)).start();
                }
            } catch (IOException e) {
                System.err.println("Server error: " + e.getMessage());
            }
        });

        serverThread.setDaemon(true);
        serverThread.start();
    }

    public static void main(String args[]) {
        launch(args);
    }

    private void openAdminInterface() {
        Platform.runLater(() -> {
            try {
                AdminConsoleApp adminApp = new AdminConsoleApp();
                adminApp.start(new Stage());
            } catch (Exception e) {
                System.err.println("Error opening client interface: " + e.getMessage());
            }
        });
    }

    private void openClientInterface() {
        Platform.runLater(() -> {
            try {
                ClientInterface clientApp = new ClientInterface();
                clientApp.start(new Stage());
            } catch (Exception e) {
                System.err.println("Error opening client interface: " + e.getMessage());
            }
        });
    }

    private List<Location> loadLocationsFromDatabase() {
        String sql = "SELECT id, name, latitude, longitude FROM locations";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String name = resultSet.getString("name");
                double latitude = resultSet.getDouble("latitude");
                double longitude = resultSet.getDouble("longitude");

                List<WeatherInfo> forecast = new ArrayList<>();

                String weatherSql = "SELECT temperature, humidity, forecast_date, condition, wind_speed FROM weather_info WHERE location_id = ?";
                try (PreparedStatement weatherStatement = connection.prepareStatement(weatherSql)) {
                    weatherStatement.setInt(1, id);
                    try (ResultSet weatherResultSet = weatherStatement.executeQuery()) {
                        while (weatherResultSet.next()) {
                            Date forecastDate = weatherResultSet.getDate("forecast_date");
                            String condition = weatherResultSet.getString("condition");
                            double temperature = weatherResultSet.getDouble("temperature");
                            double humidity = weatherResultSet.getDouble("humidity");
                            double windSpeed = weatherResultSet.getDouble("wind_speed");

                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                            String formattedDate = sdf.format(forecastDate);

                            WeatherInfo weatherInfo = new WeatherInfo(formattedDate, condition, temperature, humidity, windSpeed);
                            forecast.add(weatherInfo);
                        }
                    }
                }

                Location location = new Location(name, latitude, longitude, forecast);
                locationsDatabase.add(location);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return locationsDatabase;
    }
}