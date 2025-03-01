package Server;

import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.Scanner;

import java.sql.Connection;

public class AdminConsole implements Runnable {
    private final Server server;
    private Stage primaryStage;
    private Connection connection;

    public AdminConsole(Server server, Stage primaryStage) {
        this.server = server;
        this.primaryStage = primaryStage;
        this.connection = server.getConnection();
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("Admin Console:");
            System.out.println("1. Load data from custom file");
            System.out.println("2. Exit admin console");
            System.out.print("Choose an option: ");

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    loadCustomData();
                    break;
                case 2:
                    System.out.println("Exiting admin console...");
                    return;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private void loadCustomData() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        File selectedFile = fileChooser.showOpenDialog(primaryStage);

        if (selectedFile != null) {
            try {
                server.loadDataFromJson(selectedFile.getAbsolutePath());
                server.saveDataToDatabase();
                System.out.println("Locations loaded from JSON and saved to the database.");
            } catch (Exception e) {
                System.err.println("Error loading and saving data: " + e.getMessage());
            }
        } else {
            System.out.println("No file selected.");
        }
    }
}