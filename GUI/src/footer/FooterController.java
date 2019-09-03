package footer;

import app.AppController;
import javafx.fxml.FXML;

import javafx.scene.control.Label;



public class FooterController {

    @FXML Label dynamicMessageLabel;
    private AppController appController;

    // properties
    // TODO progress property

    public void setAppController(AppController appController) {
        this.appController = appController;
    }

    @FXML
    private void initialize() {
        // TODO write bind properties method
        //dynamicMessageLabel.textProperty().bind(dynamicStatusMessage);
    }
}