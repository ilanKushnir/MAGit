package Engine.GsonClasses;

public class RepositoryData {

    private String name;
    private String activeBranchName;
    Integer  numberOfBranches;
    private String lastCommitDate;
    private String lastCommitMessage;

    public RepositoryData(String name, String activeBranchName, Integer numberOfBranches, String lastCommitDate, String lastCommitMessage){
        this.name = name;
        this.activeBranchName = activeBranchName;
        this.numberOfBranches = numberOfBranches;
        this.lastCommitDate = lastCommitDate;
        this.lastCommitMessage = lastCommitMessage;
    }
}
