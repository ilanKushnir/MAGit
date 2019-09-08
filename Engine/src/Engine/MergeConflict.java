package Engine;

import Engine.Commons.FolderType;

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

    public void setResult(String content) { // TODO MergeConflict: check wich parameters to put on set result (with new component)
        Blob bomponentBlob = new Blob(content);
        //this.resultComponent = new Folder.Component("Name?", FolderType.FILE, "Modifier?", "Modified?", (FolderComponent)bomponentBlob);
    }

    public void setResult(Folder.Component resultComponent) {
        this.resultComponent = resultComponent;
    }
}
