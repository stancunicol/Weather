package Client;

import Common.Location;
import Common.WeatherInfo;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.List;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    public void start() {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            Scanner scanner = new Scanner(System.in);

            while (true) {
                System.out.println("Client Menu:");
                System.out.println("1. Request weather data for your location");
                System.out.println("2. Exit");
                System.out.print("Choose an option: ");

                int choice = scanner.nextInt();
                scanner.nextLine();

                switch (choice) {
                    case 1:
                        System.out.println("Enter your location:");
                        String locationName = scanner.nextLine();
                        out.writeObject(locationName);
                        out.flush();

                        Location location = (Location) in.readObject();
                        if (location != null) {
                            System.out.println("Weather data for: " + location.getName());
                            List<WeatherInfo> forecast = location.getForecast();
                            if (forecast.isEmpty()) {
                                System.out.println("No weather data available.");
                            } else {
                                forecast.forEach(f -> System.out.println("Date: " + f.getDate() +
                                        ", Condition: " + f.getCondition() +
                                        ", Temperature: " + f.getTemperature() + "°C, " +
                                        "Humidity: " + f.getHumidity() + "%, " +
                                        "Wind Speed: " + f.getWindSpeed() + " km/h"));
                            }
                        } else {
                            System.out.println("No data available for this location.");
                        }
                        break;
                    case 2:
                        System.out.println("Exiting client...");
                        return;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.start();
    }
}