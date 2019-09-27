package body.graph.layout;

import com.fxgraph.graph.Graph;
import com.fxgraph.graph.ICell;
import com.fxgraph.layout.Layout;
import body.graph.node.CommitNode;

import java.util.List;

// simple test for scattering commits in imaginary tree, where every 3rd node is in a new 'branch' (moved to the right)
public class CommitTreeLayout implements Layout {

    @Override
    public void execute(Graph graph) {
        final List<ICell> cells = graph.getModel().getAllCells();
        int startX = 10;
        int startY = 50;
        int x = 0;
        int y = 0;
        for (ICell cell : cells) {
            CommitNode c = (CommitNode) cell;

            graph.getGraphic(c).relocate(startX + c.getXPos() * 30, startY + c.getyPos() * 50);
            //graph.getGraphic(c).relocate(startX + (x), startY + y);
            x += 30;
            y += 30;
        }
    }
}
