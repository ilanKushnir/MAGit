package body;

import Engine.Blob;
import Engine.Commons.FolderType;
import Engine.Folder;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;

public class SmartTreeItem<T> extends TreeItem<T> {
    private Folder.Component data = null;

    public SmartTreeItem(final T value, final Node graphic, Folder.Component data) {
        super(value, graphic);
        this.data = data;
    }

    @Override
    public String toString() {
        if(data.getType().equals(FolderType.FILE)) {
            return ((Blob)data.getComponent()).getContent();
        } else {
            return super.toString();
        }
    }

    public Folder.Component getData() { return data; }

    public void setData(Folder.Component data) { this.data = data; }
}
