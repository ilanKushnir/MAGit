package Engine;

import Engine.Commons.CollaborationSource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Branch {
    private String name;
    private Commit lastCommit;

    //  Collaboration
    private CollaborationSource collaborationSource = CollaborationSource.LOCAL;

    public Branch(String name) {
        lastCommit = null;
        this.name = name;
    }

    public Branch(String name, Commit lastCommit) {
        this.lastCommit = lastCommit;
        this.name = name;
    }

    public Branch(String name, Commit lastCommit, CollaborationSource collaborationSource) {
        this(name, lastCommit);
        this.collaborationSource = collaborationSource;
    }

    public Branch(File file, CollaborationSource collaborationSource) throws IOException {
        this(file);
        this.collaborationSource = collaborationSource;
    }

    public Branch(File file) throws IOException {
        Commit lastCommit;
        String branchName = file.getName();
        String lastCommitSHA = Manager.readFileToString(file);
        String ancestorFolder = file.getParentFile().getParent().contains("branches")? file.getParentFile().getParentFile().getParent() : file.getParentFile().getParent();

        File lastCommitFile = new File(ancestorFolder + File.separator + "objects" + File.separator + lastCommitSHA + ".zip");
        if(!lastCommitSHA.equals("") && !lastCommitFile.exists())
            throw new FileNotFoundException("The branch '" + branchName + "' is pointing to a non existent commit");
        if (!lastCommitSHA.equals("")) {
            lastCommit = new Commit(lastCommitFile);
            this.lastCommit = lastCommit;
        } else {
            this.lastCommit = null;
        }
        this.name = branchName;
    }

    public String getName() {
        return this.name;
    }

    public Commit getCommit() {
        return this.lastCommit;
    }

    public void setLastCommit(Commit lastCommit) {
        this.lastCommit = lastCommit;
    }

    public CollaborationSource getCollaborationSource() { return this.collaborationSource;
    }

    public void setCollaborationSource(CollaborationSource collaborationSource) { this.collaborationSource = collaborationSource; }
}
