import Engine.Manager;
import app.AppController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.net.URL;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader();

        // load main fxml
        URL mainFXML = getClass().getResource("/app/app.fxml");
        loader.setLocation(mainFXML);
        AnchorPane root = loader.load();

        // wire up controller
        AppController appController = loader.getController();
        Manager model = new Manager();
        appController.setView(primaryStage);
        appController.setModel(model);

        // set stage
        primaryStage.setTitle("M.A.Git");
        Scene scene = new Scene(root, 1050, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}