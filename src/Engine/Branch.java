package Engine;

public class Branch {
    private String name;
    private Commit lastCommit;

//    boolean tracking = false;
//    boolean isRemote = false;

    public Branch(String name) {
        lastCommit = null;                 // initial commit
        this.name = name;
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
