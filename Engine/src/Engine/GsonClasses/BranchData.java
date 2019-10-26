package Engine.GsonClasses;

import Engine.Commit;

import java.util.List;
import java.util.stream.Collectors;

public class BranchData {
    private String name;
    private String pointedCommit;
    private boolean isRtb;
    private List<CommitData> branchCommits;

    BranchData(String name, String pointedCommit, boolean isRtb, List<CommitData> commitsList) {
        this.name = name;
        this.pointedCommit = pointedCommit;
        this.isRtb = isRtb;
        buildBranchCommits(commitsList);
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
