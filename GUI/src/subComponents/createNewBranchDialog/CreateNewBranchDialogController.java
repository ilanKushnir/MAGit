package subComponents.createNewBranchDialog;

import app.AppController;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.event.ActionEvent;


public class CreateNewBranchDialogController {

    private AppController appController;

    @FXML Button cancelButton;
    @FXML Button okButton;
    @FXML Label uncommitedChangesLabel;
    @FXML RadioButton checkoutNewBranchRadioButton;
    @FXML TextField newBranchNameTextField;

    @FXML
    public void initialize() {
    }

    public void setMainController(AppController appController) {
        this.appController = appController;
    }

    public void okButtonAction(ActionEvent actionEvent) {
        appController.createNewBranch(newBranchNameTextField.getText(), checkoutNewBranchRadioButton.isSelected());
        closeStage();
    }

    private void closeStage(){
        Stage stage = (Stage) okButton.getScene().getWindow();
        stage.close();
    }

    public void cancelButtonAction(ActionEvent actionEvent) {
        closeStage();
    }

    public void bindProperties() {
        uncommitedChangesLabel.visibleProperty().bind(appController.getIsUncommitedChanges());
    }
}
