package body.graph.node;

import Engine.Branch;
import Engine.Commit;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;

import java.util.HashSet;
import java.util.List;

public class CommitNodeController {

    @FXML private Label commitTimeStampLabel;
    @FXML private Label messageLabel;
    @FXML private Label committerLabel;
    @FXML private Circle CommitCircle;
    @FXML private GridPane commitTreeLine;
    @FXML private HBox pointingBranchesHBox;
    private CommitNode commitNode = null;
    private SimpleBooleanProperty isPointedByBranches;
    private HashSet<String> pointingBranches;

    public CommitNodeController() {
        isPointedByBranches = new SimpleBooleanProperty(false);
    }

    public void setCommitNode(CommitNode commitNode) {
        this.commitNode = commitNode;
    }

    public void setCommitTimeStamp(String timeStamp) {
        commitTimeStampLabel.setText(timeStamp);
        commitTimeStampLabel.setTooltip(new Tooltip(timeStamp));
    }

    public void setCommitter(String committerName) {
        committerLabel.setText(committerName);
        committerLabel.setTooltip(new Tooltip(committerName));
    }

    public void setCommitMessage(String commitMessage) {
        messageLabel.setText(commitMessage);
        messageLabel.setTooltip(new Tooltip(commitMessage));
    }

    public int getCircleRadius() {
        return (int)CommitCircle.getRadius();
    }

    public void setPointingBranches(HashSet<String> pointingBranches) {
        this.pointingBranches = pointingBranches;
        if (pointingBranches.size() > 0) {
            for(String branchName : pointingBranches) {
                Label pointingBranchLabel = new Label(branchName);
                if (commitNode.getAppController().getModel().getActiveRepository().getHEAD().getName().equals(branchName)) {
                    pointingBranchLabel.setStyle("-fx-background-color: yellow");
                }
                pointingBranchesHBox.getChildren().add(pointingBranchLabel);
            }
        }
    }


    @FXML
    private void initialize() {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem createNewBranchOnCommit = new MenuItem("New branch");

        createNewBranchOnCommit.setOnAction(e -> {
            commitNode.getAppController().createNewBranchDialog();
        });
        MenuItem setHeadBranch = new MenuItem("Set HEAD");
        setHeadBranch.setOnAction(e -> {
            // TODO tree: add reset
//            commitNode.getAppController().getModel(
        });
        Menu mergePointingBranchWithHead = new Menu("Merge branch with HEAD");
        mergePointingBranchWithHead.disableProperty().bind(isPointedByBranches.not());
        Menu deletePointingBranch = new Menu("Delete pointing branch");
        deletePointingBranch.disableProperty().bind(isPointedByBranches.not());

        contextMenu.getItems().add(createNewBranchOnCommit);
        contextMenu.getItems().add(setHeadBranch);
        contextMenu.getItems().add(mergePointingBranchWithHead);
        contextMenu.getItems().add(deletePointingBranch);
        commitTreeLine.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                contextMenu.hide();
                commitNode.getAppController().getBodyComponentController().displayCommitFilesTree(commitNode.getCommitObj());
                commitNode.getAppController().getBodyComponentController().displayCommitInfo(commitNode.getCommitObj());
                commitNode.getAppController().getBodyComponentController().selectTabInBottomTabPane("info");
            }
        });
        commitTreeLine.setOnContextMenuRequested(e -> {
            if (this.pointingBranches.size() == 0) {
                isPointedByBranches.set(false);
            } else {
                isPointedByBranches.set(true);
            }

            for (String branchName : pointingBranches) {
                Branch branch = commitNode.getAppController().getModel().getActiveRepository().getBranchByName(branchName);
                if (!branchName.equals(commitNode.getAppController().getModel().getActiveRepository().getHEAD().getName())) {
                    MenuItem mergeWithHEADButton = new MenuItem(branchName);
                    mergeWithHEADButton.setOnAction(event -> {
                        commitNode.getAppController().merge(branch);
                    });
                    MenuItem deleteBranchButton = new MenuItem(branchName);
                    deleteBranchButton.setOnAction(event -> {
                        //TODO context menu connect delete branch
                        //commitNode.getAppController().
                    });
                    mergePointingBranchWithHead.getItems().add(mergeWithHEADButton);
                    deletePointingBranch.getItems().add(deleteBranchButton);
                }
            }

            contextMenu.show(commitTreeLine, e.getScreenX(), e.getScreenY());



            commitTreeLine.requestFocus();
        });
    }



}
