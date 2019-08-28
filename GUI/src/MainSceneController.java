import Engine.Branch;
import Engine.Manager;
import Engine.Repository;
import javafx.application.HostServices;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.omg.PortableServer.POAPackage.ObjectAlreadyActive;

import javax.swing.*;
import java.awt.*;
import java.awt.TextArea;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Optional;

public class MainSceneController {

    // FXML elements
    @FXML Label           repoNameLabel;
    @FXML VBox            brnchesButtonsVBox;
    @FXML Button          toolbarPullButton;
    @FXML Button          toolbarPushButton;
    @FXML Button          toolbarFetchButton;
    @FXML MenuItem        createNewRepositoryMenuBarButton;
    @FXML MenuItem        importFromXMLMenuBarButton;
    @FXML MenuItem        loadrepositoryFromPathMenuBarButton;
    @FXML MenuItem        cloneMenuBarButton;
    @FXML MenuItem        fetchMenuBarButton;
    @FXML MenuItem        pullMenuBarButton;
    @FXML MenuItem        pushMenuBarButton;
    @FXML MenuItem        commitMenuBarButton;
    @FXML MenuItem        showStatusMenuBarButton;
    @FXML MenuItem        createNewBranchMenuBarButton;
    @FXML MenuItem        deleteBranchMenuBarButton;
    @FXML MenuItem        checkoutMenuBarButton;
    @FXML MenuItem        mergeWithMenuBarButton;
    @FXML MenuItem        resetBranchSHAMenuBarButton;
    @FXML MenuItem        showStatusSplitMenuButton;
    @FXML MenuItem        changeActiveUserTopMenuItem;
    @FXML MenuItem        changeActiveUserSideMenuItem;
    @FXML Hyperlink       accEditBranchesButton;
    @FXML Hyperlink       accNewBranchButton;
    @FXML Hyperlink       repositoryPathHyperLink;
    @FXML MenuButton      toolbarMergeWithButton;
    @FXML MenuButton      activeUserMenuButton;
    @FXML SplitMenuButton commitSplitMenuButton;
    @FXML javafx.scene.control.TextArea logTextArea;





    // properties
    private SimpleStringProperty repoPath;
    private SimpleStringProperty repoName;
    private SimpleStringProperty activeUser;
    private SimpleBooleanProperty isRepositoryLoaded;


    private Manager model;
    private Stage view;

    public MainSceneController() {
        // initilizing properties
        repoPath = new SimpleStringProperty("No repository loded");
        repoName = new SimpleStringProperty("No repository loded");
        activeUser = new SimpleStringProperty("Active user");
        isRepositoryLoaded = new SimpleBooleanProperty(false);

    }

    public void setModel(Manager model) {
        this.model = model;
        activeUser.set(model.getActiveUser());
    }

    public void setView(Stage view) { this.view = view; }

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
        activeUserMenuButton.textProperty().bind(activeUser);
        logTextArea.setEditable(false);


        // Availability
        toolbarPullButton.disableProperty().bind(isRepositoryLoaded.not());
        toolbarPushButton.disableProperty().bind(isRepositoryLoaded.not());
        toolbarFetchButton.disableProperty().bind(isRepositoryLoaded.not());
        fetchMenuBarButton.disableProperty().bind(isRepositoryLoaded.not());
        pullMenuBarButton.disableProperty().bind(isRepositoryLoaded.not());
        pushMenuBarButton.disableProperty().bind(isRepositoryLoaded.not());
        commitMenuBarButton.disableProperty().bind(isRepositoryLoaded.not());
        showStatusMenuBarButton.disableProperty().bind(isRepositoryLoaded.not());
        createNewBranchMenuBarButton.disableProperty().bind(isRepositoryLoaded.not());
        deleteBranchMenuBarButton.disableProperty().bind(isRepositoryLoaded.not());
        checkoutMenuBarButton.disableProperty().bind(isRepositoryLoaded.not());
        mergeWithMenuBarButton.disableProperty().bind(isRepositoryLoaded.not());
        resetBranchSHAMenuBarButton.disableProperty().bind(isRepositoryLoaded.not());
        showStatusSplitMenuButton.disableProperty().bind(isRepositoryLoaded.not());
        accEditBranchesButton.disableProperty().bind(isRepositoryLoaded.not());
        accNewBranchButton.disableProperty().bind(isRepositoryLoaded.not());
        repositoryPathHyperLink.disableProperty().bind(isRepositoryLoaded.not());
        toolbarMergeWithButton.disableProperty().bind(isRepositoryLoaded.not());
        commitSplitMenuButton.disableProperty().bind(isRepositoryLoaded.not());
    }

    @FXML
    public void commit() {
        TextInputDialog dialog = new TextInputDialog("");
        dialog.setTitle("Commit");
        dialog.setHeaderText("Creating new commit");
        dialog.setContentText("Please enter your commit messsage:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(commitMessage -> {
            try {
                model.commit(commitMessage);
            } catch (IOException e) {
                showExceptionStackTraceDialog(e);
            }
        });
    }

    @FXML
    public void switchActiveUser() {
        TextInputDialog dialog = new TextInputDialog("");
        dialog.setTitle("Change active user");
        dialog.setHeaderText("The active user is: " + model.getActiveUser());
        dialog.setContentText("Please enter new user name:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()){
            model.switchUser(result.get());
            activeUser.set(result.get());
        }
    }

    @FXML
    public void switchRepository() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select repository to load");
        File selectedFolder = directoryChooser.showDialog(view);

        if (selectedFolder == null) {
            return;
        }
        Path absolutePath = Paths.get(selectedFolder.getAbsolutePath());
        try{
            model.switchRepository(absolutePath);
            isRepositoryLoaded.set(true);
        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Error loading repository");
            alert.setHeaderText(ex.getMessage());
            ButtonType buttonTypeCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(buttonTypeCancel);
            alert.showAndWait();
            isRepositoryLoaded.set(false);
        }

        updateRepositoryUIAndDetails();
    }

    @FXML
    public void createNewRepository() {
        TextInputDialog dialog = new TextInputDialog("");
        dialog.setTitle("New Repository");
        dialog.setHeaderText("Setting new Repository");
        dialog.setContentText("Please enter new Repository name:");
        Optional<String> result = dialog.showAndWait();
        String repositoryName = result.get();

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select new repository url path");
        File selectedFolder = directoryChooser.showDialog(view);

        if (selectedFolder == null) {
            return;
        }
        Path absolutePath = Paths.get(selectedFolder.getAbsolutePath());
        try{
            model.createNewRepository(absolutePath, repositoryName);
            isRepositoryLoaded.set(true);
        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Existing repository");
            alert.setHeaderText(ex.getMessage());
            ButtonType buttonTypeCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(buttonTypeCancel);
            alert.showAndWait();
            isRepositoryLoaded.set(false);
        }

        updateRepositoryUIAndDetails();
    }

    @FXML
    public void importRepositoryFromXML() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select XML file");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("xml files", "*.xml"));
        File selectedFile = fileChooser.showOpenDialog(view);
        if (selectedFile == null) {
            return;
        }
        Path absolutePath = Paths.get(selectedFile.getAbsolutePath());
        try {
            model.importFromXML(absolutePath, false);
            isRepositoryLoaded.set(true);
        } catch (ObjectAlreadyActive e) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Existing repository");
            alert.setHeaderText("There is already an existing repository in the XML's path");
            alert.setContentText("Do you want to overwrite it?");
            ButtonType buttonTypeYes = new ButtonType("Yes");
            ButtonType buttonTypeNo = new ButtonType("No");
            ButtonType buttonTypeCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(buttonTypeYes, buttonTypeNo, buttonTypeCancel);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == buttonTypeYes){
                try {
                    model.importFromXML(absolutePath, true);
                    isRepositoryLoaded.set(true);
                } catch (Exception ex) {
                    isRepositoryLoaded.set(false);
                    showExceptionDialog(ex);
                }
            } else if (result.get() == buttonTypeNo) {
                String[] messages = e.getMessage().split(" ");
                try {
                    model.switchRepository(Paths.get(messages[messages.length-1]));
                    isRepositoryLoaded.set(true);
                } catch (IOException ex) {
                    isRepositoryLoaded.set(false);
                    showExceptionDialog(ex);
                }
            } else {
                isRepositoryLoaded.set(false);
            }
        } catch (Exception ex) {
            showExceptionStackTraceDialog(ex);
        }

        updateRepositoryUIAndDetails();
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
                        showExceptionDialog(e);
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










    // TODO test
    @FXML void showExceptionDialog(Exception ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Error");
        alert.setContentText(ex.getMessage());
        alert.showAndWait();
    }

    // TODO test
    @FXML
    public void showExceptionStackTraceDialog(Exception ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Exception Dialog");
        alert.setHeaderText("Look, an Exception Dialog");
        alert.setContentText("Could not find file blabla.txt!");

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        String exceptionText = sw.toString();

        Label label = new Label("The exception stacktrace was:");

        TextArea textArea;
        textArea = new TextArea(exceptionText);
        textArea.setEditable(false);

       // GridPane.setVgrow(textArea, Priority.ALWAYS);
       // GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
       // expContent.add(textArea, 0, 1);

        alert.getDialogPane().setExpandableContent(expContent);
        alert.showAndWait();
    }

}