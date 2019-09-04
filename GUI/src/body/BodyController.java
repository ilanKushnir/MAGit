package body;

import Engine.*;
import app.AppController;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import java.util.HashSet;

public class BodyController {

    // FXML elements
    @FXML private Accordion leftPaneAccordion;
    @FXML private Label repoNameLabel;
    @FXML private Hyperlink repositoryPathHyperLink;
    @FXML private TitledPane repositoryTitledPane;
    @FXML private TitledPane remoteTitledPane;
    @FXML private TitledPane branchesTitledPane;
    @FXML private VBox brnchesButtonsVBox;
    @FXML private Hyperlink accNewBranchButton;
    @FXML private TextArea logTextArea;
    @FXML private Tab infoTab;
    @FXML private Tab filesTab;
    @FXML private Tab logTab;
    @FXML private TabPane bottomTabPane;
    @FXML private TreeView commitFilesTreeView;

    // properties
    private SimpleStringProperty repoPath;
    private SimpleStringProperty repoName;
    private SimpleStringProperty activeUser;
    private SimpleBooleanProperty isRepositoryLoaded;

    private Manager model;
    private AppController appController;

    public void setAppController(AppController appController) {
        this.appController = appController;
    }
    public void setModel(Manager model) {
        this.model = model;
        activeUser.set(model.getActiveUser());
    }

    public VBox getBranchesButtonsVBox(){
        return this.brnchesButtonsVBox;
    }

    @FXML
    private void initialize() {
        logTextArea.setEditable(false);
    }

    public void bindProperties() {
        SimpleBooleanProperty isRepositoryLoaded = appController.getIsRepositoryLoaded();

        repositoryPathHyperLink.textProperty().bind(appController.getRepoPath());
        repoNameLabel.textProperty().bind(appController.getRepoName());

//        repositoryPathHyperLink.setOnAction(event -> {
//            try {
//                TODO ilan: finish it!
//                Desktop.getDesktop().open(new File(repoPath.toString()));
//            } catch (IOException e) {
//                showExceptionStackTraceDialog(e);
//            }
//        });

        repoNameLabel.disableProperty().bind(isRepositoryLoaded.not());
        accNewBranchButton.disableProperty().bind(isRepositoryLoaded.not());
        repositoryPathHyperLink.disableProperty().bind(isRepositoryLoaded.not());
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

    public void menuItemsEventHandler(ActionEvent event) {
        Object source = event.getSource();
        String id;

        appController.checkForUncommitedChanges();

        if (source instanceof Hyperlink) {
            id = ((Hyperlink) source).getId();
        } else {
            id = "";
        }

        switch(id)
        {
            case "accNewBranchButton":
                appController.createNewBranchDialog();
                break;
            default:
                System.out.println("no match");
        }
    }

    public void expandAccordionTitledPane(String paneName) {
        switch(paneName) {
            case "repository":
                this.leftPaneAccordion.setExpandedPane(repositoryTitledPane);
                break;
            case "remote":
                this.leftPaneAccordion.setExpandedPane(remoteTitledPane);
                break;
            case "branches":
                this.leftPaneAccordion.setExpandedPane(branchesTitledPane);
                break;
        }
    }

    public void selectTabInBottomTabPane(String tabName) {
        SingleSelectionModel<Tab> selectionModel = this.bottomTabPane.getSelectionModel();

        switch(tabName) {
            case "info":
                selectionModel.select(infoTab);
                break;
            case "files":
                selectionModel.select(filesTab);
                break;
            case "log":
                selectionModel.select(logTab);
                break;
        }
    }

    public void setTextAreaString(String content) {
        this.logTextArea.setText(content);
    }

    public void displayCommitFilesTree(Commit commit) {
        Node folderIcon = new ImageView(new Image(getClass().getResourceAsStream("/resources/folder_16.png")));
        TreeItem<String> rootFolder = new TreeItem<>("root", folderIcon);
        rootFolder.setExpanded(true);

        displayCommitFilesTreeRec(commit.getRootFolder(), rootFolder);
        commitFilesTreeView.setRoot(rootFolder);
    }

    private void displayCommitFilesTreeRec(Folder subFolder, TreeItem subTreeitem) {
        for(Folder.Component component : subFolder.getComponents()) {


            if (component.getType().equals(FolderType.FOLDER)) {
                Node folderIcon = new ImageView(new Image(getClass().getResourceAsStream("/resources/folder_16.png")));
                TreeItem<String> folderItem = new TreeItem<>(component.getName(), folderIcon);
                subTreeitem.getChildren().add(folderItem);
                displayCommitFilesTreeRec((Folder) component.getComponent(), folderItem);
                folderItem.setExpanded(true);
            } else if (component.getType().equals(FolderType.FILE)) {
                Node fileIcon = new ImageView(new Image(getClass().getResourceAsStream("/resources/file_16.png")));
                TreeItem<String> fileItem = new TreeItem<>(component.getName(), fileIcon);
                subTreeitem.getChildren().add(fileItem);
            }
        }
    }
}