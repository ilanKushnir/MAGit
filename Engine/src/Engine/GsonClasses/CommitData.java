package Engine.GsonClasses;

import Engine.Commit;

import java.util.LinkedList;
import java.util.List;


public class CommitData {
    private String message;
    private String SHA1;
    private String dateCreated;
    private String author;
    private TreeComponentsData tree;
    private List<String> pointingBranches = new LinkedList<>();


    CommitData(Commit commit) {
        this.message = commit.getDescription();
        this.SHA1 = commit.getSha1();
        this.dateCreated = commit.getDateCreated();
        this.author = commit.getAuthor();
        this.tree = new TreeComponentsData(commit.getTree());
    }

    public void addPointingBranch(String branchName) {
        pointingBranches.add(branchName);
    }

    public String getSHA1() {
        return SHA1;
    }

    public String getDateCreated() {
        return dateCreated;
    }

    public List<String> getPointingBranches() {
        return this.pointingBranches;
    }
}
