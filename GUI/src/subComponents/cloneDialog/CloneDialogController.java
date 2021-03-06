package subComponents.cloneDialog;

import Engine.Commons.CollaborationSource;
import app.AppController;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import javax.net.ssl.SSLParameters;
import java.io.File;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CloneDialogController {

    private AppController appController;

    @FXML TextField remoteURLTextField;
    @FXML TextField localURLTextField;
    @FXML Button remoteURLChooserButton;
    @FXML Button localURLChooserButton;
    @FXML Button cloneButton;
    @FXML Button cancelButton;

    public void remoteURLChooserButtonAction(ActionEvent actionEvent) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select repository to clone");
        File selectedFolder = directoryChooser.showDialog(appController.getView());

        if (selectedFolder == null) {
            return;
        }
        Path absolutePath = Paths.get(selectedFolder.getAbsolutePath());
        remoteURLTextField.setText(absolutePath.toString());
    }

    public void localURLChooserButtonAction(ActionEvent actionEvent) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select path to clone to");
        File selectedFolder = directoryChooser.showDialog(appController.getView());

        if (selectedFolder == null) {
            return;
        }
        Path absolutePath = Paths.get(selectedFolder.getAbsolutePath());
        localURLTextField.setText(absolutePath.toString());
    }

    public void cloneButtonAction(ActionEvent actionEvent) throws Exception {
        Path remotePath = Paths.get(remoteURLTextField.getText());
        Path localPath = Paths.get(localURLTextField.getText());
        try {
            appController.getModel().clone(remotePath, localPath);
            appController.getBodyComponentController().setTextAreaString("Clone procces completed, \nhead branch created locally and ready to work");
            appController.getIsRepositoryLoaded().set(true);
            appController.getIsRemoteRepositoryExists().set(true);
        } catch (FileSystemNotFoundException e) {
            appController.showExceptionDialog(e);
            appController.getBodyComponentController().setTextAreaString("The directory you chose to clone isnt a MAGit repository");
        } catch(Exception e) {
            appController.showExceptionDialog(e);
        } finally  {
            appController.getBodyComponentController().selectTabInBottomTabPane("log");
            if(appController.getIsRepositoryLoaded().get()) {
                appController.getBodyComponentController().expandAccordionTitledPane("remote");
                appController.getBodyComponentController().displayCommitFilesTree(appController.getModel().getActiveRepository().getHEAD().getCommit());
                appController.updateRepositoryUIAndDetails();
            }
            closeStage();
        }
    }

    public void CancelButtonAction(ActionEvent actionEvent) {
        closeStage();
    }

    private void closeStage(){
        Stage stage = (Stage) cloneButton.getScene().getWindow();
        stage.close();
    }

    public void bindProperties() {
    }

    public void setMainController(AppController appController) { this.appController = appController;
    }
}
