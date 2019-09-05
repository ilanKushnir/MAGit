package Engine;

public class MergeConflict {
    /* Conflicts types:
            1. 3 different files
            2. 2 different files + deleted
            3. 2 different added files
     */
    private Folder containingFolder;

    private String ancestorContent;
    private Blob ancestorBlob;

    private String oursContent;
    private Blob oursBlob;

    private String theirsContent;
    private Blob theirsBlob;

    public MergeConflict(Blob ancestorBlob, Blob oursBlob, Blob theirsBlob, Folder containingFolder) {
        this.ancestorBlob = ancestorBlob;
        this.oursBlob = oursBlob;
        this.theirsBlob = theirsBlob;

        ancestorContent = ancestorBlob  == null ? null : ancestorBlob.getContent();
        oursContent = oursBlob          == null ? null : oursBlob.getContent();
        theirsContent = theirsBlob      == null ? null : theirsBlob.getContent();

        this.containingFolder = containingFolder;
    }

    public String getAncestorContent() { return this.ancestorContent;
    }

    public Blob getAncestorBlob() { return this.ancestorBlob;
    }

    public String getOursContent() { return this.oursContent;
    }

    public Blob getOursBlob() { return this.oursBlob;
    }

    public String getTheirsContent() { return this.theirsContent;
    }

    public Blob getTheirsBlob() { return this.theirsBlob;
    }

    public Folder getContainingFolder() { return this.containingFolder;
    }
}
