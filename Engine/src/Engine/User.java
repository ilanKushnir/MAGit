package Engine;

import Engine.Commons.Constants;
import Engine.GsonClasses.PullRequestData;
import Engine.GsonClasses.RepositoryData;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

public class User {
    private String userName;
    private Manager manager;
    private HashSet<RepositoryData> repositories;
    private HashMap<String, String> forkedRepositories = new HashMap<>();   //  <UserName, ForekedRepoName>
    private LinkedList<PullRequestData> pullRequestsData = new LinkedList<>();

    public User(String userName) {
        this.userName = userName;
        repositories = new HashSet<>();
        manager = new Manager();
        manager.switchUser(userName);
    }

    public void addPullRequest(String author, String repositoryName, String targetBranch, String baseBranch, String description) {
        String date = Manager.getCurrentDateString();
        pullRequestsData.addFirst(new PullRequestData("pr-" + this.pullRequestsData.size(), author, date, repositoryName, targetBranch, baseBranch, description));
    }

    public LinkedList<PullRequestData> getPullRequestsData() {
        return pullRequestsData;
    }

    public PullRequestData getPullRequestByID(String id) {
        PullRequestData out = null;
        for (PullRequestData pullRequestData : this.pullRequestsData) {
            if(pullRequestData.getId().equals(id)){
                out = pullRequestData;
            }
        }

        return out;
    }

    public void addForkedRepository(String username, String forkedName) {
        forkedRepositories.put(username, forkedName);
    }

    public HashMap<String, String> getForkedRepositories() {
        return forkedRepositories;
    }

    public User(String userName, HashSet<RepositoryData> repositories) {
        this(userName);
        this.repositories = repositories;
    }

    //getters
    public HashSet<RepositoryData> getRepositories() { return this.repositories; }
    public String getUserName() { return this.userName; }
    public Manager getManager() { return this.manager; }

    //setters
    public void setRepositories(HashSet<RepositoryData> repositories) { this.repositories = repositories; }

    public void addRepository(RepositoryData newRepository) throws FileAlreadyExistsException {
        repositories.add(newRepository);

//        String repoDirString = Constants.MAGITHUB_FOLDER_PATH + File.separator + this.userName + File.separator + newRepository.getName();
//        File repoDir = new File(repoDirString);
//
//        if(!repoDir.exists()){
//            repoDir.mkdir();
//            repositories.add(newRepository);
//        } else {
//            throw new FileAlreadyExistsException("Repository with the same name already exists in this account.");
//        }

    }

    public void addNewRepositoryData(String repositoryName) {
        Integer numberOfBranches;
        String activeBranchName = null;
        String lastCommitDate = null;
        String lastCommitMessage = null;
        String lastCommitSha1;
        String repositoryPath = Constants.MAGITHUB_FOLDER_PATH + File.separator + this.userName + File.separator + repositoryName;
        String repositoryBranchesPath = repositoryPath + File.separator + ".magit" + File.separator + "branches";

        numberOfBranches = new File(repositoryBranchesPath).listFiles().length - 1;

        try {
            activeBranchName = Manager.readFileToString(repositoryBranchesPath + File.separator + "HEAD");
            lastCommitSha1 = Manager.readFileToString(repositoryBranchesPath + File.separator + activeBranchName);
            Commit lastCommit = new Commit(new File(repositoryPath + File.separator + ".magit" + File.separator + "objects" + File.separator + lastCommitSha1 + ".zip"));
            lastCommitDate = lastCommit.getDateCreated();
            lastCommitMessage = lastCommit.getDescription();
        } catch (IOException e) {
            e.printStackTrace();
        }

        repositories.add(new RepositoryData(repositoryName, activeBranchName, numberOfBranches, lastCommitDate, lastCommitMessage));
    }
}
