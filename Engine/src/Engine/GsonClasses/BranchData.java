package Engine.GsonClasses;

public class BranchData {
    private String name;
    private String pointedCommit;
    private boolean isRtb;

    BranchData(String name, String pointedCommit, boolean isRtb) {
        this.name = name;
        this.pointedCommit = pointedCommit;
        this.isRtb = isRtb;
    }
}
