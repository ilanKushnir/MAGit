package subComponents.conflictsDialog;

import Engine.MergeConflict;
import app.AppController;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import jdk.jfr.events.ExceptionThrownEvent;

public class ConflictsDialogController {

    private AppController appController;

    @FXML Button cancelButton;
    @FXML Button solveButton;
    @FXML ListView<MergeConflict> conflictsListView;

    @FXML
    public void initialize() {
    }

    public void setMainController(AppController appController) {
        this.appController = appController;
    }

    public void solveButtonAction(ActionEvent actionEvent) {
        appController.solveConflict(conflictsListView.getSelectionModel().getSelectedItem(), conflictsListView.getItems());
        closeStage();
    }

    private void closeStage(){
        Stage stage = (Stage) solveButton.getScene().getWindow();
        stage.close();
    }

    public void cancelButtonAction(ActionEvent actionEvent) {
        appController.getIsMergeFinished().set(false);
        closeStage();
    }

    public void bindProperties() {
        solveButton.disableProperty().bind(conflictsListView.getSelectionModel().selectedItemProperty().isNull());
    }

    public ListView<MergeConflict> getConflictsListView() { return this.conflictsListView;
    }
}
