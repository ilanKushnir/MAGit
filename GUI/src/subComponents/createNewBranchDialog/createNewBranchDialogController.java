package subComponents.createNewBranchDialog;

import MainSceneController;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.awt.event.ActionEvent;

public class createNewBranchDialogController {

    private Stage view;
    private MainSceneController mainController;

    @FXML Button cancelButton;
    @FXML Button okButton;
    @FXML RadioButton checkoutNewBranchRadioButton;
    @FXML TextField textField;



    @FXML
    public void initialize() {
    }

    public void setMainController(MainSceneController mainController) {
        this.mainController = mainController;
    }


    public void okButtonAction(ActionEvent actionEvent) {
        mainController.CreateNewBranch(textField.getText(),checkoutNewBranchRadioButton.isSelected() == Boolean.TRUE);
        closeStage();
    }

    private void closeStage(){
        Stage stage = (Stage) okButton.getScene().getWindow();
        stage.close();
    }

    public void cancelButtonAction(ActionEvent actionEvent) {
        closeStage();
    }
}
