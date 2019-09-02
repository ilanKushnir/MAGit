package body;

import Engine.Branch;
import Engine.Manager;
import Engine.Repository;
import app.AppController;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.omg.PortableServer.POAPackage.ObjectAlreadyActive;
import subComponents.createNewBranchDialog.CreateNewBranchDialogController;

import java.awt.TextArea;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Optional;

public class BodyController {

    // FXML elements
    @FXML Label           repoNameLabel;
    @FXML VBox            brnchesButtonsVBox;
    @FXML Hyperlink       accEditBranchesButton;
    @FXML Hyperlink       accNewBranchButton;
    @FXML Hyperlink       repositoryPathHyperLink;
    @FXML javafx.scene.control.TextArea logTextArea;


    // properties
    private SimpleStringProperty repoPath;
    private SimpleStringProperty repoName;
    private SimpleStringProperty activeUser;
    private SimpleBooleanProperty isRepositoryLoaded;

    private Manager model;

    private AppController appController;

    public BodyController() {
        // initilizing properties
        repoPath = new SimpleStringProperty("No repository loded");
        repoName = new SimpleStringProperty("No repository loded");
        activeUser = new SimpleStringProperty("Active user");
        isRepositoryLoaded = new SimpleBooleanProperty(false);
    }

    public void setAppController(AppController appController) {
        this.appController = appController;
    }

    public void setModel(Manager model) {
        this.model = model;
        activeUser.set(model.getActiveUser());
    }

    @FXML
    private void initialize() {
        repositoryPathHyperLink.textProperty().bind(repoPath);
//        repositoryPathHyperLink.setOnAction(event -> {
//            try {
//                TODO ilan: finish it!
//                Desktop.getDesktop().open(new File(repoPath.toString()));
//            } catch (IOException e) {
//                showExceptionStackTraceDialog(e);
//            }
//        });

        repoNameLabel.textProperty().bind(repoName);
        logTextArea.setEditable(false);
    }

    public VBox getBranchesButtonsVBox(){
        return this.brnchesButtonsVBox;
    }

    // TODO use bindings
    public void updateRepositoryUIAndDetails() {
        if(isRepositoryLoaded.get()) {
            Repository activeRepository = model.getActiveRepository();
            repoPath.set(activeRepository.getRootPath().toString());
            repoName.set(activeRepository.getName());
            updateBranchesButtons();
        }
    }

    private void updateBranchesButtons() {
        createBranchesSideCheckoutButtons();
        //updateBranchMergeButtons();
    }

    private void createBranchesSideCheckoutButtons() {
        if(isRepositoryLoaded.get()) {
            Repository activeRepository = model.getActiveRepository();
            HashSet<Branch> branchesSet = activeRepository.getBranches();
            brnchesButtonsVBox.getChildren().clear();
            for (Branch branch : branchesSet) {
                String branchName = branch.getName();
                Button branchButton = new Button();
                branchButton.setText(branchName);
                branchButton.setOnAction(event -> {
                    try {
                        model.checkout(branchName);
                        updateBranchesSideCheckoutButtons();
                    } catch (Exception e) {
                        appController.showExceptionDialog(e);
                    }
                });
                brnchesButtonsVBox.getChildren().add(branchButton);
                updateBranchesSideCheckoutButtons();
            }
        }
    }

    private void updateBranchesSideCheckoutButtons() {
        String HEADname = model.getActiveRepository().getHEAD().getName();
        for (Node node : brnchesButtonsVBox.getChildren()) {
            if (node instanceof Button) {
                Button button = (Button) node;
                button.setDisable(button.getText().equals(HEADname));
            }
        }
    }



}