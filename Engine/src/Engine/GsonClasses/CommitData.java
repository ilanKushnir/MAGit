package Engine.GsonClasses;

import Engine.Commit;

import java.util.List;


public class CommitData {
    private String message;
    private String SHA1;
    private String dateCreated;
    private String author;
    private TreeData tree;
    List<String> pointingBranches;          // TODO CommitData: add a list of commit pointing branches


    CommitData(Commit commit) {
        this.message = commit.getDescription();
        this.SHA1 = commit.getSha1();
        this.dateCreated = commit.getDateCreated();
        this.author = commit.getAuthor();
        this.tree = new TreeData(commit.getTree());

    }


}
