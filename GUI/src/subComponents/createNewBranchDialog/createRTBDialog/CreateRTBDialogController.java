package subComponents.createNewBranchDialog.createRTBDialog;

import Engine.Branch;
import app.AppController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class CreateRTBDialogController {

    private AppController appController;

    @FXML Button cancelButton;
    @FXML Button okButton;
    @FXML Label uncommitedChangesLabel;
    @FXML RadioButton checkoutNewBranchRadioButton;
    @FXML RadioButton stayOnCurrentBranchRadioButton;
    @FXML ChoiceBox branchChooserChoiceBox;

    public void setMainController(AppController appController) { this.appController = appController;  }

    public ChoiceBox getBranchesChoiceBox() { return this.branchChooserChoiceBox; }

    public void okButtonAction(ActionEvent actionEvent) {
        appController.createLocalRTB((Branch) branchChooserChoiceBox.getValue(), checkoutNewBranchRadioButton.isSelected());
        closeStage();
    }

    public void cancelButtonAction(ActionEvent actionEvent) {
        closeStage();
    }

    private void closeStage() {
        Stage stage = (Stage) okButton.getScene().getWindow();
        stage.close();
    }

    public void bindProperties() {
        uncommitedChangesLabel.visibleProperty().bind(appController.getIsUncommitedChanges());
        stayOnCurrentBranchRadioButton.selectedProperty().bind(checkoutNewBranchRadioButton.selectedProperty().not());
        checkoutNewBranchRadioButton.selectedProperty().bind(stayOnCurrentBranchRadioButton.selectedProperty().not());
    }
}
