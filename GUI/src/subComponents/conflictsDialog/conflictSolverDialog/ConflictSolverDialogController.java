package subComponents.conflictsDialog.conflictSolverDialog;

import Engine.Commons.FolderType;
import Engine.Folder;
import Engine.FolderComponent;
import Engine.MergeConflict;
import app.AppController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;



public class ConflictSolverDialogController {

    private AppController appController;
    private MergeConflict conflict;

    @FXML Button    acceptAncestorButton;
    @FXML Button    acceptOursButton;
    @FXML Button    acceptTheirsButton;
    @FXML Button    acceptButton;
    @FXML TextArea  ancestorTextArea;
    @FXML TextArea  oursTextArea;
    @FXML TextArea  theirsTextArea;
    @FXML TextField resultTextField;

    public void bindProperties() {
    }

    public void setMainController(AppController appController) { this.appController = appController;
    }

    public TextArea getAncestorTextArea() { return ancestorTextArea;
    }

    public void setAncestorTextArea(TextArea ancestorTextArea) { this.ancestorTextArea = ancestorTextArea;
    }

    public TextArea getOursTextArea() { return oursTextArea;
    }

    public void setOursTextArea(TextArea oursTextArea) { this.oursTextArea = oursTextArea;
    }

    public TextArea getTheirsTextArea() { return theirsTextArea;
    }

    public void setTheirsTextArea(TextArea theirsTextArea) { this.theirsTextArea = theirsTextArea;
    }

    public TextField getResultTextField() { return resultTextField;
    }

    public void acceptAncestorButtonAction  (ActionEvent actionEvent) {
        conflict.setResult(conflict.getAncestorComponent());
        conflict.setSolved(true);
        closeStage();
    }

    public void acceptOursButtonAction      (ActionEvent actionEvent) {
        conflict.setResult(conflict.getOursComponent());
        conflict.setSolved(true);
        closeStage();
    }

    public void acceptTheirsButtonAction    (ActionEvent actionEvent) {
        conflict.setResult(conflict.getTheirsComponent());
        conflict.setSolved(true);
        closeStage();
    }

    public void acceptButtonAction          (ActionEvent actionEvent) {
        String fileName = conflict.getAncestorComponent() != null ? conflict.getAncestorComponent().getName()
                : conflict.getOursComponent() != null ? conflict.getOursComponent().getName()
                : conflict.getTheirsComponent() != null ? conflict.getTheirsComponent().getName()
                : null;
        conflict.setResult(fileName, appController.getModel().getActiveUser(), resultTextField.getText() );
        conflict.setSolved(true);
        closeStage();
    }

    public void setConflict(MergeConflict conflict) { this.conflict = conflict;
    }

    private void closeStage() {
        Stage stage = (Stage) appController.getConflictSolverDialogScene().getWindow();
        stage.close();
    }
}
