package subComponents.mergeDialog;

import app.AppController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;


public class MergeDialogController {

    private AppController appController;

    @FXML Button cancelButton;
    @FXML Button okButton;
    @FXML Label headBranchNameLabel;
    @FXML ChoiceBox branchesChoiceBox;

    @FXML
    public void initialize() {
    }

    public void setMainController(AppController appController) {
        this.appController = appController;
    }

    public void okButtonAction(ActionEvent actionEvent) {
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
    }

    //TODO add branches list to branchesChoiceBox!
}
