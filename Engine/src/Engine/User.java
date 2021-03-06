package Engine;

import Engine.Commons.Constants;
import Engine.GsonClasses.CommitData;
import Engine.GsonClasses.PullRequestData;
import Engine.GsonClasses.RepositoryData;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.text.ParseException;
import java.util.*;

public class User {
    private String userName;
    private Manager manager;
    private HashSet<RepositoryData> repositories;
    private HashMap<String, LinkedList<String>> forkedRepositories = new HashMap<>();   //  <UserName, ForekedRepoName>
    private NotificationsCenter notificationsCenter = new NotificationsCenter();

    private LinkedList<PullRequestData> pullRequestsData = new LinkedList<>();
    private Boolean shouldSwitch = false;

    public User(String userName) {
        this.userName = userName;
        repositories = new HashSet<>();
        manager = new Manager();
        manager.switchUser(userName);
    }

    public void addPullRequest(String author, String repositoryName, String targetBranch, String baseBranch, String description, String statusLogString, LinkedList<CommitData> commitsDataList) {
        String date = Manager.getCurrentDateString();
        pullRequestsData.addFirst(new PullRequestData("pr-" + this.pullRequestsData.size(), author, date, repositoryName, targetBranch, baseBranch, description, statusLogString, commitsDataList));
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

    public NotificationsCenter getNotificationsCenter() {
        return notificationsCenter;
    }

    public void addForkedRepository(String username, String forkedName) {
        if(forkedRepositories.containsKey(username)) {
            forkedRepositories.get(username).add(forkedName);
        } else {
            LinkedList<String> list = new LinkedList<>();
            list.add(forkedName);
            forkedRepositories.put(username, list);
        }
    }

    public HashMap<String, LinkedList<String>> getForkedRepositories() {
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
    public Boolean getShouldSwitch() { return shouldSwitch;
    }

    //setters
    public void setRepositories(HashSet<RepositoryData> repositories) { this.repositories = repositories; }

    public void setShouldSwitch(Boolean shouldSwitch) { this.shouldSwitch = shouldSwitch;
    }

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

    public void addNewRepositoryData(Repository repository) throws IOException, ParseException {
        repositories.add(new RepositoryData(repository));
    }

    public String getPRStatusLog(String targetBranchName, String baseBranchName) throws IOException {
        Commit targetCommit = manager.getActiveRepository().getBranchByName(targetBranchName).getCommit();
        Commit baseCommit = manager.getActiveRepository().getBranchByName(baseBranchName).getCommit();

        return manager.getCommitsStatusLogDiff(targetCommit, baseCommit).toString();
    }

    public LinkedList<CommitData> getCommitDeltaList(String targetBranchName, String baseBranchName) throws IOException {
        LinkedList<CommitData> commitsDataDeltaList = new LinkedList<>();

        Commit targetCommit = manager.getActiveRepository().getBranchByName(targetBranchName).getCommit();
        Commit baseCommit = manager.getActiveRepository().getBranchByName(baseBranchName).getCommit();

        boolean flag1 = addCommitsDataToDeltaListRec(targetCommit.getParentCommitSHA(), commitsDataDeltaList, baseCommit.getSha1());
        boolean flag2 = addCommitsDataToDeltaListRec(targetCommit.getotherParentCommitSHA(), commitsDataDeltaList, baseCommit.getSha1());

        if (flag1 || flag2) {
            commitsDataDeltaList.add(new CommitData(targetCommit));
        }

        Collections.reverse(commitsDataDeltaList);
        return commitsDataDeltaList;
    }

    private boolean addCommitsDataToDeltaListRec(String currCommitSHA1, LinkedList<CommitData> commitsDataDeltaList, String baseCommitSHA1) throws IOException {
        if (currCommitSHA1.equals(baseCommitSHA1)) {
            return true;
        }
        if (currCommitSHA1.equals("")) {
            return false;
        }

        String commitFilePath = manager.getActiveRepository().getRootPath() + File.separator + ".magit" + File.separator + "objects" + File.separator + currCommitSHA1 + ".zip";
        File commitFile = new File(commitFilePath);
        Commit currCommit = new Commit(commitFile);
        boolean gotToBase = false;

        gotToBase = addCommitsDataToDeltaListRec(currCommit.getParentCommitSHA(), commitsDataDeltaList, baseCommitSHA1);
        if (gotToBase) {
            commitsDataDeltaList.add(new CommitData(currCommit));
            return true;
        }

        gotToBase = addCommitsDataToDeltaListRec(currCommit.getotherParentCommitSHA(), commitsDataDeltaList, baseCommitSHA1);
        if (gotToBase) {
            commitsDataDeltaList.add(new CommitData(currCommit));
            return true;
        }

        return false;
    }
}
