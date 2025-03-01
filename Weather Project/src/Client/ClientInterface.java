package Client;

import Common.Location;

import Common.WeatherInfo;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.application.Platform;
import java.util.List;

import java.io.*;
import java.net.Socket;

public class ClientInterface extends Application {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Client");

        Label locationLabel = new Label("Enter your location:");
        TextField locationInput = new TextField();
        Button requestWeatherButton = new Button("Request Weather");
        TextArea weatherOutput = new TextArea();
        weatherOutput.setEditable(false);

        Label coordinatesLabel = new Label("Enter coordinates if no location is found:");
        coordinatesLabel.setVisible(false);
        TextField latitudeInput = new TextField();
        latitudeInput.setPromptText("Latitude");
        latitudeInput.setVisible(false);
        TextField longitudeInput = new TextField();
        longitudeInput.setPromptText("Longitude");
        longitudeInput.setVisible(false);
        Button findClosestLocationButton = new Button("Find Closest Location");
        findClosestLocationButton.setVisible(false);

        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Days");
        yAxis.setLabel("Temperature (°C)");

        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(0);
        xAxis.setUpperBound(7);

        LineChart<Number, Number> weatherChart = new LineChart<>(xAxis, yAxis);
        weatherChart.setTitle("Weather Forecast");

        requestWeatherButton.setOnAction(e -> {
            String location = locationInput.getText();
            if (!location.isEmpty()) {
                String weatherInfo = requestWeatherData(location);
                Platform.runLater(() -> {
                    if (weatherInfo != null && !weatherInfo.isEmpty() && !weatherInfo.equals("No information about this location.")) {
                        weatherOutput.setText(weatherInfo);
                        Location locationData = getLocationDataFromServer(location);
                        updateWeatherChart(weatherChart, locationData);

                        coordinatesLabel.setVisible(false);
                        latitudeInput.setVisible(false);
                        longitudeInput.setVisible(false);
                        findClosestLocationButton.setVisible(false);
                    } else {
                        weatherOutput.setText("No information about this location.");
                        System.out.println("Location not found. Showing coordinate inputs.");
                        coordinatesLabel.setVisible(true);
                        latitudeInput.setVisible(true);
                        longitudeInput.setVisible(true);
                        findClosestLocationButton.setVisible(true);
                    } if(weatherInfo == null) {
                        weatherOutput.setText("Error fetching weather data. Please try again.");
                        coordinatesLabel.setVisible(false);
                        latitudeInput.setVisible(false);
                        longitudeInput.setVisible(false);
                        findClosestLocationButton.setVisible(false);
                    }
                });
            } else {
                weatherOutput.setText("Please enter a location.");
                updateWeatherChart(weatherChart, null);
            }
        });

        findClosestLocationButton.setOnAction(e -> {
            String latitudeStr = latitudeInput.getText();
            String longitudeStr = longitudeInput.getText();
            if (!latitudeStr.isEmpty() && !longitudeStr.isEmpty()) {
                try {
                    double latitude = Double.parseDouble(latitudeStr);
                    double longitude = Double.parseDouble(longitudeStr);
                    Location closestLocation = requestClosestLocation(latitude, longitude);
                    Platform.runLater(() -> {
                        if (closestLocation != null) {
                            StringBuilder weatherInfo = new StringBuilder("Closest location: " + closestLocation.getName() + "\n");
                            List<WeatherInfo> forecast = closestLocation.getForecast();
                            for (WeatherInfo info : forecast) {
                                weatherInfo.append(info.getDate()).append(": ")
                                        .append(info.getCondition()).append(", ")
                                        .append(info.getTemperature()).append("°C, ")
                                        .append(info.getHumidity()).append("% humidity, ")
                                        .append(info.getWindSpeed()).append(" km/h wind speed\n");
                            }
                            weatherOutput.setText(weatherInfo.toString());
                            updateWeatherChart(weatherChart, closestLocation);
                        } else {
                            weatherOutput.setText("No locations found near these coordinates.");
                        }
                    });
                } catch (NumberFormatException ex) {
                    weatherOutput.setText("Invalid coordinates. Please enter numeric values.");
                }
            } else {
                weatherOutput.setText("Please enter both latitude and longitude.");
            }
        });

        VBox vbox = new VBox(10, locationLabel, locationInput, requestWeatherButton, weatherOutput, coordinatesLabel, latitudeInput, longitudeInput, findClosestLocationButton, weatherChart);
        vbox.setStyle("-fx-padding: 10; -fx-alignment: center;");

        Scene scene = new Scene(vbox, 500, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private String requestWeatherData(String location) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject(location);
            out.flush();

            Location result = (Location) in.readObject();
            if (result != null) {
                StringBuilder weatherData = new StringBuilder("Weather data for: " + result.getName() + "\n");
                List<WeatherInfo> forecast = result.getForecast();
                for (WeatherInfo info : forecast) {
                    weatherData.append(info.getDate()).append(": ")
                            .append(info.getCondition()).append(", ")
                            .append(info.getTemperature()).append("°C, ")
                            .append(info.getHumidity()).append("% humidity, ")
                            .append(info.getWindSpeed()).append(" km/h wind speed\n");
                }
                return weatherData.toString();
            } else {
                return "No information about this location.";
            }
        } catch (IOException e) {
            showServerNotRunningAlert();
            return null;
        }
        catch (ClassNotFoundException e) {
            return "Error fetching weather data: " + e.getMessage();
        }
    }

    private void showServerNotRunningAlert() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Server not running");
        alert.setHeaderText("The server is not running.");
        alert.setContentText("Please make sure the server is started before trying again.");

        ButtonType retryButton = new ButtonType("Retry");
        ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(retryButton, closeButton);

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                boolean connected = false;
                while(!connected)
                {
                    try {
                        connected = tryToConnect();
                    } catch (IOException e) {
                        alert.setContentText("Connection failed. Retrying...");
                        alert.showAndWait();
                    }
                }
            }
            else if(response == closeButton) {
                Platform.exit();
            }
        });
    }

    private void updateWeatherChart(LineChart<Number, Number> chart, Location locationData) {
        chart.getData().clear();

        if (locationData != null && locationData.getForecast() != null && !locationData.getForecast().isEmpty()) {
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName(locationData.getName() + " Forecast");

            for (int i = 0; i < locationData.getForecast().size(); i++) {
                double temperature = locationData.getForecast().get(i).getTemperature();
                series.getData().add(new XYChart.Data<>(i + 1, temperature));
            }

            chart.getData().add(series);
        }
    }

    private boolean tryToConnect() throws IOException {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private Location getLocationDataFromServer(String location) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject(location);
            out.flush();

            return (Location) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            showServerNotRunningAlert();
            return null;
        }
    }

    private Location requestClosestLocation(double latitude, double longitude) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            String request = "COORDINATES," + latitude + "," + longitude;
            out.writeObject(request);
            out.flush();

            return (Location) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}