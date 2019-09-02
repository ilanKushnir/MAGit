package header;

import app.AppController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.MenuItem;

public class HeaderController {

    // FXML elements
    @FXML MenuItem createNewRepositoryMenuBarButton;

    private AppController appController;

    public HeaderController() {
    }

    public void setAppController(AppController appController) {
        this.appController = appController;
    }

    @FXML
    private void initialize() {
    }

    @FXML
    private void createNewBranchButtonAction(ActionEvent actionEvent){
        appController.createNewBranchDialog();
    }
}