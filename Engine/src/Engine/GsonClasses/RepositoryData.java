package Engine.GsonClasses;

import Engine.Commit;
import Engine.Manager;
import Engine.Repository;

import java.util.ArrayList;
import java.util.List;

public class RepositoryData {

    private String name;
    private String activeBranchName;
    private Integer numberOfBranches;
    private String lastCommitDate;
    private String lastCommitMessage;
    private List<CommitData> commitsList = new ArrayList<>();

    public RepositoryData(String name, String activeBranchName, Integer numberOfBranches, String lastCommitDate, String lastCommitMessage){
        this.name = name;
        this.activeBranchName = activeBranchName;
        this.numberOfBranches = numberOfBranches;
        this.lastCommitDate = lastCommitDate;
        this.lastCommitMessage = lastCommitMessage;
    }

    public RepositoryData(Repository repository, List<Commit> commits) {
        this.name = repository.getName();
        this.activeBranchName = repository.getHEAD().getName();
        this.numberOfBranches = repository.getBranches().size();
        this.lastCommitDate = repository.getHEAD().getCommit().getDateCreated();
        this.lastCommitMessage = repository.getHEAD().getCommit().getDescription();
        commits.forEach(commit ->
                this.commitsList.add(new CommitData(commit)));
    }
}
