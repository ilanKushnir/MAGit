package Engine;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Branch {
    private String name;
    private Commit lastCommit;
//    boolean tracking = false;
//    boolean isRemote = false;

    public Branch(String name) {
        lastCommit = null;
        this.name = name;
    }

    public Branch(String name, Commit lastCommit) {
        this.lastCommit = lastCommit;
        this.name = name;
    }

    public Branch(File file) throws IOException {
        Commit lastCommit;
        String branchName = file.getName();
        String lastCommitSHA = Manager.readFileToString(file);

        File lastCommitFile = new File(file.getParentFile().getParent() + "//objects//" + lastCommitSHA + ".zip");
        if(!lastCommitFile.exists())
            throw new FileNotFoundException("The branch '" + branchName + "' is pointing to a non existent commit");

        lastCommit = new Commit(lastCommitFile);

        this.name = branchName;
        this.lastCommit = lastCommit;
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


}
