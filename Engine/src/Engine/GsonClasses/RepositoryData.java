package Engine.GsonClasses;

import Engine.Branch;
import Engine.Commit;
import Engine.Manager;
import Engine.Repository;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

public class RepositoryData {

    private String name;
    private String activeBranchName;
    private Integer numberOfBranches;
    private String lastCommitDate;
    private String lastCommitMessage;
    private List<CommitData> commitsList = new LinkedList<>();

    public RepositoryData(String name, String activeBranchName, Integer numberOfBranches, String lastCommitDate, String lastCommitMessage){
        this.name = name;
        this.activeBranchName = activeBranchName;
        this.numberOfBranches = numberOfBranches;
        this.lastCommitDate = lastCommitDate;
        this.lastCommitMessage = lastCommitMessage;
    }

    public RepositoryData(Repository repository) throws ParseException, IOException {
        Commit latestCommit = repository.getLatestCommit();
        this.name = repository.getName();
        this.activeBranchName = repository.getHEAD().getName();
        this.numberOfBranches = repository.getBranches().size();
        this.lastCommitDate = latestCommit.getDateCreated();
        this.lastCommitMessage = latestCommit.getDescription();
        buildCommitsList(repository, this.commitsList);
    }

    private void buildCommitsList(Repository repository, List<CommitData> commitsList) throws IOException {
        for (Branch branch : repository.getBranches()) {
            addCommitsToListRec(branch.getCommit(), commitsList, repository.getRootPath().toString() + File.separator + ".magit" + File.separator + "objects");
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
    }

    private void addCommitsToListRec(Commit commit, List<CommitData> commitsList, String objectsPath) throws IOException {
        if (commit == null) {
            return;
        }

        commitsList.add(new CommitData(commit));

        File firstParentcommitFile = new File(objectsPath + File.separator + commit.getParentCommitSHA());
        File secondParentcommitFile = new File(objectsPath + File.separator + commit.getotherParentCommitSHA());

        Commit firstParentCommit = null;
        Commit secondParentCommit = null;

        if(firstParentcommitFile.exists()) {
            firstParentCommit = new Commit(firstParentcommitFile);
        }
        if (secondParentcommitFile.exists()) {
            secondParentCommit = new Commit(secondParentcommitFile);
        }

        addCommitsToListRec(firstParentCommit, commitsList, objectsPath);
        addCommitsToListRec(secondParentCommit, commitsList, objectsPath);
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
