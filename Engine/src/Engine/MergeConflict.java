package Engine;

import Engine.Commons.FolderType;
import Engine.Manager;

public class MergeConflict {
    /* Conflicts types:
            1. 3 different files
            2. 2 different files + deleted
            3. 2 different added files
     */
    private Folder containingFolder;

    private String ancestorContent;
    private Folder.Component ancestorComponent;

    private String oursContent;
    private Folder.Component oursComponent;

    private String theirsContent;
    private Folder.Component theirsComponent;

    private Folder.Component resultComponent = null;
    private boolean solved = false;

    public MergeConflict(Folder.Component ancestorComponent, Folder.Component oursComponent, Folder.Component theirsComponent, Folder containingFolder) {
        this.ancestorComponent = ancestorComponent;
        this.oursComponent = oursComponent;
        this.theirsComponent = theirsComponent;

        ancestorContent = ancestorComponent == null ? null : ((Blob)ancestorComponent.getComponent()).getContent();
        oursContent = oursComponent         == null ? null : ((Blob)oursComponent.getComponent()).getContent();
        theirsContent = theirsComponent     == null ? null : ((Blob)theirsComponent.getComponent()).getContent();

        this.containingFolder = containingFolder;
    }

    public String getAncestorContent() { return this.ancestorContent;
    }

    public Folder.Component getAncestorComponent() {
        return ancestorComponent;
    }

    public String getOursContent() { return this.oursContent;
    }

    public Folder.Component getOursComponent() {
        return oursComponent;
    }

    public String getTheirsContent() { return this.theirsContent;
    }

    public Folder.Component getTheirsComponent() {
        return theirsComponent;
    }

    public Folder getContainingFolder() { return this.containingFolder;
    }

    public Folder.Component getResultComponent() { return this.resultComponent;
    }

    public void setResult(String name, String lastModifier , String content) {
        Blob componentBlob = new Blob(content);
        String lastModified = Manager.getCurrentDateString();
        Folder folder = new Folder();
        this.resultComponent = folder.new Component(name, FolderType.FILE, lastModifier, lastModified, componentBlob);
    }

    public void setResult(Folder.Component resultComponent) {
        this.resultComponent = resultComponent;
    }

    public boolean isSolved() { return this.solved;
    }

    public void setSolved(boolean solved) { this.solved = solved;
    }
}













