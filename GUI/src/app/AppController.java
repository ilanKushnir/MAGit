package app;

import Engine.Branch;
import Engine.Manager;
import Engine.Repository;
import body.BodyController;
import footer.FooterController;
import header.HeaderController;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.omg.PortableServer.POAPackage.ObjectAlreadyActive;
import subComponents.createNewBranchDialog.CreateNewBranchDialogController;
import java.awt.TextArea;
import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;

public class AppController {

    // model & view
    private Manager model;
    private Stage view;

    // sub components
    @FXML private AnchorPane headerComponent;
    @FXML private HeaderController headerComponentController;
    @FXML private AnchorPane bodyComponent;
    @FXML private BodyController bodyComponentController;
    @FXML private AnchorPane footerComponent;
    @FXML private FooterController footerComponentController;

    @FXML private CreateNewBranchDialogController createNewBranchDialogController;
    private Scene createNewBranchDialogScene;

    // properties
    private SimpleStringProperty repoPath;
    private SimpleStringProperty repoName;
    private SimpleStringProperty activeUser;
    private SimpleBooleanProperty isRepositoryLoaded;

    public AppController() {
        // initilizing properties
        repoPath = new SimpleStringProperty("No repository loded");
        repoName = new SimpleStringProperty("No repository loded");
        activeUser = new SimpleStringProperty("Active user");
        isRepositoryLoaded = new SimpleBooleanProperty(false);
    }

    // setters
    public void setHeaderComponentController(HeaderController headerController) { this.headerComponentController = headerController; }
    public void setBodyComponentController(BodyController bodyController) { this.bodyComponentController = bodyController; }
    public void setFooterComponentController(FooterController footerController) { this.footerComponentController = footerController; }
    public void setView(Stage view) { this.view = view; }
    public void setModel(Manager model) {
        this.model = model;
        activeUser.set(model.getActiveUser());
    }

    // getters
    public Manager getModel() {
        return this.model;
    }
    public SimpleStringProperty getRepoPath() {
        return this.repoPath;
    }
    public SimpleStringProperty getRepoName() {
        return this.repoName;
    }
    public SimpleBooleanProperty getIsRepositoryLoaded() {
        return this.isRepositoryLoaded;
    }

    @FXML
    private void initialize() {
        if(headerComponentController != null && bodyComponentController != null && footerComponentController != null) {
            headerComponentController.setAppController(this);
            bodyComponentController.setAppController(this);
            footerComponentController.setAppController(this);
        }

        initializeDialogComponents();
    }

    private void initializeDialogComponents() {
        FXMLLoader loader;

        try {
            // load new branch controller
            loader = new FXMLLoader();
            URL dialogFXML = getClass().getResource("/subComponents/createNewBranchDialog/createNewBranchDialog.fxml");
            loader.setLocation(dialogFXML);
            AnchorPane dialogRoot = loader.load();
            createNewBranchDialogScene = new Scene(dialogRoot);
            createNewBranchDialogController = loader.getController();
            createNewBranchDialogController.setMainController(this);
        } catch (IOException e) {
            showExceptionDialog(e);
        }

    }

    @FXML
    public void createNewBranchDialog() {
        Stage stage = new Stage();
        stage.setTitle("Create new branch");
        stage.setScene(createNewBranchDialogScene);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.showAndWait();
    }

    @FXML
    public void createNewBranch(String branchName, boolean shouldCheckout) {
        try {
            model.createNewBranch(branchName);
        } catch (Exception e) {
            showExceptionDialog(e);
        }

        if (shouldCheckout) {
            try {
                model.checkout(branchName);
            } catch (Exception e) {
                showExceptionDialog(e);
            }
        }

        updateRepositoryUIAndDetails();
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
    public void checkout(String branchName) {
        try {
            model.checkout(branchName);
        } catch (Exception e) {
            showExceptionDialog(e);
        }
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
            bodyComponentController.getBranchesButtonsVBox().getChildren().clear();
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
                bodyComponentController.getBranchesButtonsVBox().getChildren().add(branchButton);
                updateBranchesSideCheckoutButtons();
            }
        }
    }

    private void updateBranchesSideCheckoutButtons() {
        String HEADname = model.getActiveRepository().getHEAD().getName();
        for (Node node : bodyComponentController.getBranchesButtonsVBox().getChildren()) {
            if (node instanceof Button) {
                Button button = (Button) node;
                button.setDisable(button.getText().equals(HEADname));
            }
        }
    }










    // TODO test
    @FXML
    public void showExceptionDialog(Exception ex) {
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