package subComponents.checkOutDialog;

import app.AppController;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class CheckOutDialogController {

    private AppController appController;

    @FXML
    Button cancelButton;
    @FXML Button okButton;
    @FXML
    Label headBranchNameLabel;
    @FXML
    ChoiceBox branchesChoiceBox;

    public void setMainController(AppController appController) { this.appController = appController; }

    public void okButtonAction(ActionEvent actionEvent) {
        appController.checkout((String) branchesChoiceBox.getValue());
        closeStage();
    }

    private void closeStage(){
        Stage stage = (Stage) okButton.getScene().getWindow();
        stage.close();
    }

    public ChoiceBox getBranchesChoiceBox() { return this.branchesChoiceBox; }

    public void cancelButtonAction(ActionEvent actionEvent) {
        closeStage();
    }

    public void bindProperties() {
        SimpleStringProperty headName = appController.getHeadBranch();
        headBranchNameLabel.textProperty().bind(headName);
    }

}
