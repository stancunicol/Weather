package Server;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class AdminConsoleApp extends Application {
    private Server server;
    private Label statusLabel;
    private String jsonFilePath;
    private TextField locationTextField;
    private Button saveDataButton;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        server = new Server();

        locationTextField = new TextField();
        locationTextField.setPromptText("Enter location name");

        Button loadCustomDataButton = new Button("Load Locations from JSON");
        loadCustomDataButton.setOnAction(e -> loadLocationsFromJson(primaryStage));

        saveDataButton = new Button("Save Data to Database");
        saveDataButton.setOnAction(e -> {
            if (server.getLocationsDatabase().isEmpty()) {
                statusLabel.setText("No locations loaded. Please load data first.");
            } else {
                try {
                    server.saveDataToDatabase();
                    statusLabel.setText("Data saved to database successfully.");
                } catch (Exception ex) {
                    statusLabel.setText("Error saving data to database: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        });

        statusLabel = new Label("Status: Waiting for actions...");

        VBox vbox = new VBox(10, loadCustomDataButton, saveDataButton, statusLabel);
        vbox.setStyle("-fx-padding: 10; -fx-alignment: center;");

        Scene scene = new Scene(vbox, 400, 250);
        primaryStage.setTitle("Admin Console");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void loadLocationsFromJson(Stage primaryStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));

        File selectedFile = fileChooser.showOpenDialog(primaryStage);

        if (selectedFile != null) {
            jsonFilePath = selectedFile.getAbsolutePath();
            try {
                server.loadDataFromJson(jsonFilePath);
                statusLabel.setText("Locations loaded from JSON: " + jsonFilePath);
            } catch (Exception ex) {
                statusLabel.setText("Error loading locations from JSON: " + ex.getMessage());
                ex.printStackTrace();
            }
        } else {
            statusLabel.setText("No file selected.");
        }
    }
}