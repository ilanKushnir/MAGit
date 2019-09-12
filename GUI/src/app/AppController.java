package app;

import Engine.*;
import body.BodyController;
import footer.FooterController;
import header.HeaderController;
import javafx.beans.property.*;
import javafx.event.Event;
import javafx.event.EventHandler;
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
import javafx.util.StringConverter;
import org.omg.PortableServer.POAPackage.ObjectAlreadyActive;
import subComponents.createNewBranchDialog.CreateNewBranchDialogController;
import subComponents.mergeDialog.MergeDialogController;

import java.awt.TextArea;
import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
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

    // dialog componenets
    @FXML private CreateNewBranchDialogController createNewBranchDialogController;
    private Scene createNewBranchDialogScene;
    @FXML private MergeDialogController mergeDialogController;
    private Scene mergeDialogScene;

    // properties
    private SimpleStringProperty repoPath;
    private SimpleStringProperty repoName;
    private SimpleStringProperty activeUser;
    private SimpleBooleanProperty isRepositoryLoaded;
    private SimpleBooleanProperty isRemoteRepositoryExists;
    private SimpleBooleanProperty isUncommitedChanges;
//    private SimpleListProperty<Branch> branches;

    public AppController() {
        // initilizing properties
        repoPath = new SimpleStringProperty("No repository loded");
        repoName = new SimpleStringProperty("No repository loded");
        activeUser = new SimpleStringProperty("Active user");
        isRepositoryLoaded = new SimpleBooleanProperty(false);
        isRemoteRepositoryExists = new SimpleBooleanProperty(false);
        isUncommitedChanges = new SimpleBooleanProperty(false);
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
    public SimpleStringProperty getRepoPath() { return this.repoPath; }
    public SimpleStringProperty getRepoName() {
        return this.repoName;
    }
    public SimpleStringProperty getActiveUser() { return this.activeUser; }
    public SimpleBooleanProperty getIsRemoteRepositoryExists() { return this.isRemoteRepositoryExists; }
    public SimpleBooleanProperty getIsRepositoryLoaded() {
        return this.isRepositoryLoaded;
    }
    public SimpleBooleanProperty getIsUncommitedChanges() {
        return this.isUncommitedChanges;
    }

    @FXML
    private void initialize() {
        if(headerComponentController != null && bodyComponentController != null && footerComponentController != null) {
            headerComponentController.setAppController(this);
            bodyComponentController.setAppController(this);
            footerComponentController.setAppController(this);
        }

        initializeDialogComponents();
        bindSubComponentsProperties();

        bodyComponentController.showCommitsGraph();
    }

    private void bindSubComponentsProperties() {
        headerComponentController.bindProperties();
        bodyComponentController.bindProperties();
        createNewBranchDialogController.bindProperties();
        mergeDialogController.bindProperties();
    }   // footer??

    private void initializeDialogComponents() {
        FXMLLoader loader;

        try {
            // load new branch controller
            loader = new FXMLLoader();
            URL createNewBranchDialogFXML = getClass().getResource("/subComponents/createNewBranchDialog/createNewBranchDialog.fxml");
            loader.setLocation(createNewBranchDialogFXML);
            AnchorPane dialogRoot = loader.load();
            createNewBranchDialogScene = new Scene(dialogRoot);
            createNewBranchDialogController = loader.getController();
            createNewBranchDialogController.setMainController(this);

            // load merge controller
            loader = new FXMLLoader();
            URL mergeDialogFXML = getClass().getResource("/subComponents/mergeDialog/mergeDialog.fxml");
            loader.setLocation(mergeDialogFXML);
            AnchorPane mergeDialogRoot = loader.load();
            mergeDialogScene = new Scene(mergeDialogRoot);
            mergeDialogController = loader.getController();
            mergeDialogController.setMainController(this);
        } catch (IOException e) {
            showExceptionDialog(e);
        }
    }

    @FXML
    public void createNewBranchDialog() {
        checkForUncommitedChanges();
        Stage stage = new Stage();
        stage.setTitle("Create new branch");
        stage.setScene(createNewBranchDialogScene);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.showAndWait();

        bodyComponentController.expandAccordionTitledPane("branches");
    }

    @FXML
    public void mergeDialog() {
        checkForUncommitedChanges();
        Stage stage = new Stage();
        stage.setTitle("Merge");
        stage.setScene(mergeDialogScene);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.showAndWait();

        bodyComponentController.expandAccordionTitledPane("branches");
    }

    public void merge(Branch theirBranch) {
        try {
            Folder tree = new Folder();
            List<MergeConflict> conflicts = new LinkedList<MergeConflict>();

            model.merge(theirBranch, tree, conflicts);
            // TODO mergeUI: conflict solver

            model.mergeUpdateWC(tree, conflicts);
            commit();
        } catch (Exception e) {
            showExceptionDialog(e);
        }
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
                StatusLog statusLog = model.commit(commitMessage);
                bodyComponentController.setTextAreaString(statusLog.toString());
                bodyComponentController.selectTabInBottomTabPane("log");
            } catch (IOException e) {
                showExceptionStackTraceDialog(e);
            }
        });

        bodyComponentController.displayCommitFilesTree(model.getActiveRepository().getHEAD().getCommit());
    }

    @FXML
    public void showStatus() {
        try {
            String repoStatus = model.showStatus().toString();
            bodyComponentController.setTextAreaString(repoStatus);
            bodyComponentController.selectTabInBottomTabPane("log");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void checkout(String branchName) {
        try {
            checkForUncommitedChanges();
            if (isUncommitedChanges.get()) {
                if (yesNoCancelDialog(
                        "Uncommited changes",
                        "There are uncommited changes\nSome data may be lost!",
                        "Checkout anyway?"
                )) {
                    model.checkout(branchName);
                }
            } else {
                model.checkout(branchName);
            }
        } catch (Exception e) {
            showExceptionDialog(e);
        }

        bodyComponentController.expandAccordionTitledPane("branches");
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

        if(isRepositoryLoaded.get()) {
            bodyComponentController.expandAccordionTitledPane("repository");
            bodyComponentController.displayCommitFilesTree(model.getActiveRepository().getHEAD().getCommit());
            updateRepositoryUIAndDetails();
        }
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

        if(isRepositoryLoaded.get()) {
            bodyComponentController.expandAccordionTitledPane("repository");
            bodyComponentController.displayCommitFilesTree(model.getActiveRepository().getHEAD().getCommit());
            updateRepositoryUIAndDetails();
        }
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
            if (yesNoCancelDialog(
                    "Existing repository",
                    "There is already an existing repository in the XML's path",
                    "Do you want to overwrite it?"
            )){
                try {
                    model.importFromXML(absolutePath, true);
                    isRepositoryLoaded.set(true);
                } catch (Exception ex) {
                    isRepositoryLoaded.set(false);
                    showExceptionDialog(ex);
                }
            } else {
                isRepositoryLoaded.set(false);
            }

            if(isRepositoryLoaded.get()) {
                bodyComponentController.expandAccordionTitledPane("repository");
                bodyComponentController.displayCommitFilesTree(model.getActiveRepository().getHEAD().getCommit());
                updateRepositoryUIAndDetails();
            }
        } catch (Exception ex) {
            showExceptionStackTraceDialog(ex);
        }
    }

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
        updateMergeBranchButtons();
        //TODO updateMergeBranchButtons
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
                        this.checkout(branchName);
                        updateBranchesSideCheckoutButtons();
                        updateMergeBranchButtons();
                    } catch (Exception e) {
                        showExceptionDialog(e);
                    }
                });
                bodyComponentController.getBranchesButtonsVBox().getChildren().add(branchButton);
                updateBranchesSideCheckoutButtons();
            }
        }
    }

    private void updateMergeBranchButtons() {
        if(isRepositoryLoaded.get()) {
            Repository activeRepository = model.getActiveRepository();
            HashSet<Branch> branches = activeRepository.getBranches();
            // update branchList on Header Merge as well

            ChoiceBox branchChooser = mergeDialogController.getBranchesChoiceBox();
            branchChooser.getItems().clear();
            for(Branch branch: branches) {
                if(!branch.equals(model.getActiveRepository().getHEAD())) { // for each branch except the HEAD branch
                    String branchName = branch.getName();
                    branchChooser.getItems().add(branch);
                }
            }

            branchChooser.setConverter(new StringConverter<Branch>() {
                @Override
                public String toString(Branch branch) {
                    return branch.getName();
                }

                @Override
                public Branch fromString(String string) {
                    return null;
                }
            });
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

    public void checkForUncommitedChanges() {
        if (isRepositoryLoaded.get()) {
            try {
                this.isUncommitedChanges.set(!model.showStatus().isEmptyLog());
            } catch (IOException e) {
                e.printStackTrace();
            };
        }
    }




    public boolean yesNoCancelDialog(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        ButtonType buttonTypeYes = new ButtonType("Yes");
        ButtonType buttonTypeNo = new ButtonType("No");
        ButtonType buttonTypeCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(buttonTypeYes, buttonTypeNo, buttonTypeCancel);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == buttonTypeYes) {
            return true;
        } else {
            return false;
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

//        GridPane.setVgrow(textArea, Priority.ALWAYS);
//        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
//        expContent.add(textArea, 0, 1);

        alert.getDialogPane().setExpandableContent(expContent);
        alert.showAndWait();
    }

}