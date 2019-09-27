package body;

import Engine.*;
import Engine.Commons.FolderType;
import app.AppController;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

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

    public void displayCommitInfo(Commit commit) {
        // TODO show commit info
    }

    public void setInfoTextArea(String content) {
        // TODO set info text area and add it in scene builder
//        this.infoTextArea.setText(content);
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

        commitFilesTreeView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<SmartTreeItem<String>>() {
            @Override
            public void changed(ObservableValue<? extends SmartTreeItem<String>> observable, SmartTreeItem<String> oldValue, SmartTreeItem<String> newValue) {
                SmartTreeItem<String> selectedItem = newValue;
                appController.getBodyComponentController().setTextAreaString(selectedItem.getValue() + " content: \n\n" + selectedItem.toString());
                selectTabInBottomTabPane("log");
            }
        });

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
                SmartTreeItem<String> fileItem = new SmartTreeItem<>(component.getName(), fileIcon, component);
                subTreeitem.getChildren().add(fileItem);
            }
        }
    }


    public void showCommitTree() throws IOException {
        commitsTree = new Graph();
        HashMap<Commit, Integer> commitsIndexes = new HashMap<>();
        LinkedList<Commit> allCommits = appController.getModel().getCommitsList(commitsIndexes);
        addCommitsToGraph(commitsTree, allCommits, commitsIndexes);
        addEdgesToGraph(commitsTree);

        commitsTree.endUpdate();

        commitsTree.layout(new CommitTreeLayout());

        PannableCanvas canvas = commitsTree.getCanvas();
        commitsGraphScrollPane.setContent(canvas);

        Platform.runLater(() -> {
            commitsTree.getUseViewportGestures().set(false);
            commitsTree.getUseNodeGestures().set(false);
        });
    }


    private void addCommitsToGraph(Graph graph, LinkedList<Commit> commitsList, HashMap<Commit, Integer> commitIndexes) {
        final Model model = commitsTree.getModel();
        Integer yPos = 0;

        commitsTree.beginUpdate();

        for(Commit commit : commitsList) {

            HashSet<String> pointingBranches = new HashSet<>();
            for (Branch branch : appController.getModel().getActiveRepository().getBranches()) {
                if (branch.getCommit().generateSHA().equals(commit.generateSHA())) {
                    pointingBranches.add(branch.getName());
                }
            }

            ICell commitCell = new CommitNode(
                    commit.getDateCreated(),
                    commit.getAuthor(),
                    commit.getDescription(),
                    commit.getSha1(),
                    commit.getParentCommitSHA(),
                    commit.getotherParentCommitSHA(),
                    commit,
                    this.appController,
                    pointingBranches
            );

            ((CommitNode)commitCell).setyPos(yPos++);
            ((CommitNode)commitCell).setxPos(commitIndexes.get(commit));

            model.addCell(commitCell);
        }

        commitsTree.endUpdate();

    }

    private void addEdgesToGraph(Graph tree) {
        final Model model = tree.getModel();

        //tree.beginUpdate();

        for (ICell commitCell : model.getAllCells()) {
            CommitNode commitNode = (CommitNode) commitCell;
            String firstParentCommitSHA1 = commitNode.getFirstPrevCommitSHA1();
            String secondParentCommitSHA1 = commitNode.getSecondPrevCommitSHA1();
            ICell firstParentCommitNode = getCommitCellBySHA1(firstParentCommitSHA1, tree);
            ICell secondParentCommitNode = getCommitCellBySHA1(secondParentCommitSHA1, tree);

            if (firstParentCommitNode != null) {
                final Edge firstEdge = new Edge(commitCell, firstParentCommitNode);
                model.addEdge(firstEdge);
            }
            if (secondParentCommitNode != null) {
                final Edge secondEdge = new Edge(commitCell, secondParentCommitNode);
                model.addEdge(secondEdge);
            }
        }

        //tree.endUpdate();
    }

    private ICell getCommitCellBySHA1(String SHA1, Graph tree) {
        ICell out = null;

        final Model model = tree.getModel();
        for (ICell commitCell : model.getAllCells()) {
            CommitNode commitNode = (CommitNode) commitCell;
            if (commitNode.getSHA1().equals(SHA1)) {
                return commitCell;
            }
        }

        return out;
    }

}