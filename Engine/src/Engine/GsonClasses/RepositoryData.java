package Engine.GsonClasses;

import Engine.Branch;
import Engine.Commit;
import Engine.Commons.CollaborationSource;
import Engine.Manager;
import Engine.Repository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.*;

public class RepositoryData {

    private String name;
    private String activeBranchName;
    private Integer numberOfBranches;
    private String lastCommitDate;
    private String lastCommitMessage;
    private List<BranchData> branchesDataList = new LinkedList<>();
    private List<CommitData> commitsList = new LinkedList<>();

    // Leave this constructor to use it on main page without building commits
    public RepositoryData(String name, String activeBranchName, Integer numberOfBranches, String lastCommitDate, String lastCommitMessage){
        this.name = name;
        this.activeBranchName = activeBranchName;
        this.numberOfBranches = numberOfBranches;
        this.lastCommitDate = lastCommitDate;
        this.lastCommitMessage = lastCommitMessage;
    }

    // A constructor for full repo data
    public RepositoryData(Repository repository) throws ParseException, IOException {
        Commit latestCommit = repository.getLatestCommit();
        this.name = repository.getName();
        this.activeBranchName = repository.getHEAD().getName();
        this.numberOfBranches = repository.getBranches().size();
        this.lastCommitDate = latestCommit.getDateCreated();
        this.lastCommitMessage = latestCommit.getDescription();
        buildCommitsDataList(repository);
        buildBranchesDataList(repository);
    }

    private void buildBranchesDataList(Repository repository) {
        for (Branch branch : repository.getBranches()) {
            boolean isRtb = (branch.getCollaborationSource() == CollaborationSource.REMOTE);
            branchesDataList.add(new BranchData(branch.getName(), branch.getCommit().getSha1(), isRtb));
        }
    }

    private void buildCommitsDataList(Repository repository) throws IOException {
        for (Branch branch : repository.getBranches()) {
            addCommitsToListRec(branch.getCommit(), commitsList, repository.getRootPath().toString() + File.separator + ".magit" + File.separator + "objects");
        }

        // remove duplicates by SHA1
        HashMap<String, CommitData> commitsDataMap = new HashMap<>();
        for (CommitData commitData : commitsList) {
            commitsDataMap.put(commitData.getSHA1(), commitData);
        }
        commitsList.clear();
        for (CommitData commitData : commitsDataMap.values()) {
            commitsList.add(commitData);
        }

        Collections.sort(commitsList, new Comparator<CommitData>() {
            public int compare(CommitData c1, CommitData c2) {
                try {
                    Date c1Date = Manager.getDateFromFormattedDateString(c1.getDateCreated());
                    Date c2Date = Manager.getDateFromFormattedDateString(c2.getDateCreated());
                    return c1Date.compareTo(c2Date);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                return 0;
            }
        });
        Collections.reverse(commitsList);
    }

    private void addCommitsToListRec(Commit commit, List<CommitData> commitsList, String objectsPath) throws IOException {
        if (commit == null) {
            return;
        }

        commitsList.add(new CommitData(commit));

        String parentCommitSHA = commit.getParentCommitSHA();
        String secondParentCommtSHA = commit.getotherParentCommitSHA();
        Path parentCommitPath = Paths.get(objectsPath + File.separator + parentCommitSHA + ".zip");
        Path secondCommitPath = Paths.get(objectsPath + File.separator + secondParentCommtSHA + ".zip");

        if (!parentCommitSHA.equals("")) {

            File firstParentcommitFile = new File(parentCommitPath.toString());
            Commit firstParentCommit = new Commit(firstParentcommitFile);
            addCommitsToListRec(firstParentCommit, commitsList, objectsPath);
        }
        if (!secondParentCommtSHA.equals("")) {
            File secondParentcommitFile = new File(secondCommitPath.toString());
            Commit secondParentCommit = new Commit(secondParentcommitFile);
            addCommitsToListRec(secondParentCommit, commitsList, objectsPath);
        }
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
