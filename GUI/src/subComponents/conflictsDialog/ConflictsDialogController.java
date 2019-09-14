package subComponents.conflictsDialog;

import app.AppController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

public class ConflictsDialogController {

    private AppController appController;

    @FXML Button cancelButton;
    @FXML Button solveButton;
    @FXML ListView conflictsListView;

    @FXML
    public void initialize() {
    }

    public void setMainController(AppController appController) {
        this.appController = appController;
    }

    public void solveButtonAction(ActionEvent actionEvent) {
        // TODO conflicts: add conflictsSolver reference from appcontroller with chosen conflict
        closeStage();
        //update conflictList
    }

    private void closeStage(){
        Stage stage = (Stage) solveButton.getScene().getWindow();
        stage.close();
    }

    public void cancelButtonAction(ActionEvent actionEvent) {
        closeStage();
    }

    public void bindProperties() {
    }

    public ListView getConflictsListView() { return this.conflictsListView;
    }
}
