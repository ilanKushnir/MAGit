package Engine.GsonClasses;

import Engine.Commit;
import Engine.Commons.CollaborationSource;

import java.util.List;
import java.util.stream.Collectors;

public class BranchData {
    private String name;
    private String pointedCommit;
    private String collaborationSource;
    private List<CommitData> branchCommits;

    BranchData(String name, String pointedCommit, CollaborationSource collaborationSource, List<CommitData> commitsList) {
        this.name = name;
        this.pointedCommit = pointedCommit;
        buildBranchCommits(commitsList);
        switch (collaborationSource) {
            case LOCAL:
                this.collaborationSource = "local";
                break;
            case REMOTE:
                this.collaborationSource = "remote";
                break;
            case REMOTETRACKING:
                this.collaborationSource = "remotetracking";
                break;
            default:
                this.collaborationSource = "";
        }
    }

    private void buildBranchCommits(List<CommitData> commitsList) {
        branchCommits = commitsList.stream()
                .filter(commit -> commit.getPointingBranches().contains(name))
                .collect(Collectors.toList());
    }

    public String getName() {
        return name;
    }

    public String getPointedCommit() {
        return pointedCommit;
    }
}
