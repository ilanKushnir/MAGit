package footer;

import app.AppController;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;

import javafx.scene.control.Label;



public class FooterController {

    @FXML Label dynamicMessageLabel;
    private AppController appController;

    // properties
    private SimpleStringProperty dynamicStatusMessage;
    // TODO progress property

    public FooterController() {
        dynamicStatusMessage = new SimpleStringProperty("No action has been made.");
    }

    public void setDynamicMessageProperty(String message) {
        this.dynamicStatusMessage.set(message);
    }

    public void setAppController(AppController appController) {
        this.appController = appController;
    }

    @FXML
    private void initialize() {
        //dynamicMessageLabel.textProperty().bind(dynamicStatusMessage);
    }
}