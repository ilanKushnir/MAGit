package body.graph.node;

import com.fxgraph.cells.AbstractCell;
import com.fxgraph.graph.Graph;
import com.fxgraph.graph.IEdge;
import javafx.beans.binding.DoubleBinding;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;

import java.io.IOException;
import java.net.URL;

public class CommitNode extends AbstractCell {

    private String timestamp;
    private String committer;
    private String message;
    private String SHA1;
    private String firstPrevCommitSHA1;
    private String secondPrevCommitSHA1;
    private CommitNodeController commitNodeController;

    public CommitNode(String timestamp, String committer, String message, String SHA1, String firstPrevCommitSHA1, String secondPrevCommitSHA1) {
        this.timestamp = timestamp;
        this.committer = committer;
        this.message = message;
        this.SHA1 = SHA1;
        this.firstPrevCommitSHA1  = firstPrevCommitSHA1;
        this.secondPrevCommitSHA1 = secondPrevCommitSHA1;
    }

    public String getSHA1() {
        return this.SHA1;
    }
    public String getFirstPrevCommitSHA1() {
        return this.firstPrevCommitSHA1;
    }
    public String getSecondPrevCommitSHA1() {
        return this.secondPrevCommitSHA1;
    }

    @Override
    public Region getGraphic(Graph graph) {

        try {

            FXMLLoader fxmlLoader = new FXMLLoader();
            URL url = getClass().getResource("commitNode.fxml");
            fxmlLoader.setLocation(url);
            GridPane root = fxmlLoader.load(url.openStream());

            commitNodeController = fxmlLoader.getController();
            commitNodeController.setCommitMessage(message);
            commitNodeController.setCommitter(committer);
            commitNodeController.setCommitTimeStamp(timestamp);

            return root;
        } catch (IOException e) {
            return new Label("Error when tried to create graphic node !");
        }
    }

    @Override
    public DoubleBinding getXAnchor(Graph graph, IEdge edge) {
        final Region graphic = graph.getGraphic(this);
        return graphic.layoutXProperty().add(commitNodeController.getCircleRadius());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CommitNode that = (CommitNode) o;

        return timestamp != null ? timestamp.equals(that.timestamp) : that.timestamp == null;
    }

    @Override
    public int hashCode() {
        return timestamp != null ? timestamp.hashCode() : 0;
    }
}
