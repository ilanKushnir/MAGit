package Engine;

import Engine.Commons.PRStatus;

public class PullRequest {
    private String id;
    private String author;
    private String repositoryName;
    private String targetBranch;
    private String baseBranch;
    private String description;
    private PRStatus status;

    public PullRequest(String id, String author, String repositoryName, String targetBranch, String baseBranch, String description) {
        this.id = id;
        this.author = author;
        this.repositoryName = repositoryName;
        this.targetBranch = targetBranch;
        this.baseBranch = baseBranch;
        this.description = description;
        this.status = PRStatus.OPEN;
    }

    public String getId() {
        return id;
    }

    public String getAuthor() {
        return author;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public String getTargetBranch() {
        return targetBranch;
    }

    public String getBaseBranch() {
        return baseBranch;
    }

    public String getDesctiption() {
        return description;
    }

    public PRStatus getStatus() {
        return status;
    }

    public void approvePR() {
        this.status = PRStatus.APPROVED;
    }

    public void declinePR() {
        this.status = PRStatus.DECLINED;
    }
}
