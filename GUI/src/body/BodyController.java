package body;

import Engine.*;
import Engine.Commons.FolderType;
import app.AppController;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.*;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;

// commits graph imports
import com.fxgraph.edges.Edge;
import com.fxgraph.graph.Graph;
import com.fxgraph.graph.ICell;
import com.fxgraph.graph.Model;
import com.fxgraph.graph.PannableCanvas;
import body.graph.layout.CommitTreeLayout;
import body.graph.node.CommitNode;

public class BodyController {

    // FXML elements
    @FXML private Accordion leftPaneAccordion;
    @FXML private Label repoNameLabel;
    @FXML private Hyperlink repositoryPathHyperLink;
    @FXML private Label remoteRepoNameLabel;
    @FXML private Hyperlink remoteRepositoryPathHyperLink;
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
    @FXML private ScrollPane commitsGraphScrollPane;

    // properties
    private SimpleStringProperty repoPath;
    private SimpleStringProperty repoName;
    private SimpleStringProperty remoteRepoPath;
    private SimpleStringProperty remoteRepoName;
    private SimpleStringProperty activeUser;
    private SimpleBooleanProperty isRepositoryLoaded;
    private SimpleBooleanProperty isRemoteRepositoryExists;

    private Manager model;
    private AppController appController;
    private Graph commitsTree;

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
        createCommitsGraph();
    }

    public void bindProperties() {
        isRepositoryLoaded = appController.getIsRepositoryLoaded();
        isRemoteRepositoryExists = appController.getIsRemoteRepositoryExists();
        repoPath = appController.getRepoPath();
        repoName = appController.getRepoName();
        remoteRepoName = appController.getRemoteRepoName();
        remoteRepoPath = appController.getRemoteRepoPath();
        activeUser = appController.getActiveUser();

        repositoryPathHyperLink.textProperty().bind(repoPath);
        repoNameLabel.textProperty().bind(repoName);
        repoNameLabel.disableProperty().bind(isRepositoryLoaded.not());
        accNewBranchButton.disableProperty().bind(isRepositoryLoaded.not());
        repositoryPathHyperLink.disableProperty().bind(isRepositoryLoaded.not());

        remoteRepositoryPathHyperLink.textProperty().bind(remoteRepoPath);
        remoteRepoNameLabel.textProperty().bind(remoteRepoName);
        remoteRepositoryPathHyperLink.disableProperty().bind(isRemoteRepositoryExists.not());
        remoteRepoNameLabel.disableProperty().bind(isRemoteRepositoryExists.not());
    }

    public void openRepoFolderInLocalExplorer() {
        try {
            Desktop.getDesktop().open(new File(repoPath.toString()));
        } catch (IOException e) {
            appController.showExceptionStackTraceDialog(e);
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
                if(isRemoteRepositoryExists.get()) {
                    if (appController.yesNoCancelDialog(
                            "Creating new branch",
                            "You can make a local Remote Tracking Branch out of existing Remote Branch or you can create a new one",
                            "Would you like a Remote Tracking Branch?"
                    )) {
                        appController.createRTBDialog();
                    } else {
                        appController.createNewBranchDialog();
                    }
                } else {
                    appController.createNewBranchDialog();
                }
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
                fileItem.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent> () {
                    @Override
                    public void handle(MouseEvent event) {
                        logTextArea.setText(((Blob)component.getComponent()).getContent());
                        selectTabInBottomTabPane("log");
                    }
                });  //TODO show file content on LOG section when chosing it from the files tree (creat inherited object to TreeItem which can hold content)
                subTreeitem.getChildren().add(fileItem);
            }
        }
    }


    public void createCommitsGraph() {
        commitsTree = new Graph();
        createCommitNodes();

        PannableCanvas canvas = commitsTree.getCanvas();
        commitsGraphScrollPane.setContent(canvas);

        Platform.runLater(() -> {
            commitsTree.getUseViewportGestures().set(false);
            commitsTree.getUseNodeGestures().set(false);
        });
    }

    private void createCommitNodes() {
        Graph graph = commitsTree;
        final Model model = graph.getModel();

        graph.beginUpdate();

        ICell c1 = new CommitNode("20.07.2019 | 22:36:57", "Menash", "initial commit");
        ICell c2 = new CommitNode("21.07.2019 | 22:36:57", "Moyshe Ufnik", "developing some feature");
        ICell c3 = new CommitNode("20.08.2019 | 22:36:57", "Old Majesty, The FU*!@N Queen of england", "A very long commit that aims to see if and where the line will be cut and how it will look a like... very Interesting");
        ICell c4 = new CommitNode("20.09.2019 | 13:33:57", "el professore", "yet another commit");
        ICell c5 = new CommitNode("30.10.2019 | 11:36:54", "bella chao", "merge commit of 'yet another commit' and other commit");

        model.addCell(c1);
        model.addCell(c2);
        model.addCell(c3);
        model.addCell(c4);
        model.addCell(c5);

        final Edge edgeC12 = new Edge(c1, c2);
        model.addEdge(edgeC12);

        final Edge edgeC23 = new Edge(c2, c4);
        model.addEdge(edgeC23);

        final Edge edgeC45 = new Edge(c4, c5);
        model.addEdge(edgeC45);

        final Edge edgeC13 = new Edge(c1, c3);
        model.addEdge(edgeC13);

        final Edge edgeC35 = new Edge(c3, c5);
        model.addEdge(edgeC35);

        graph.endUpdate();

        graph.layout(new CommitTreeLayout());

    }
}