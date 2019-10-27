package Engine;

import Engine.Commons.CollaborationSource;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.HashSet;

public class Repository {

    private Path rootPath;
    private Branch HEAD;
    private HashSet<Branch> branches;
    // Collaboration
    private CollaborationSource collaborationSource = CollaborationSource.LOCAL;
    private Path remotePath = null;


    public Repository(Path rootPath, Branch head, HashSet<Branch> branches, Path remotePath, CollaborationSource collaborationSource) { // Ctor for remote Repository
        this(rootPath, head, branches);
        this.collaborationSource = collaborationSource;
        this.remotePath = remotePath;
    }
    public Repository(Path rootPath, Branch head) {
        this.rootPath = rootPath;
        this.HEAD = head;
        branches = new HashSet<Branch>();
        branches.add(HEAD);
    }

    public Repository(Path rootPath) {
        Branch master = new Branch("master");
        HashSet<Branch> branches = new HashSet<>();
        branches.add(master);

        this.rootPath = rootPath;
        this.branches = branches;
        this.HEAD = master;
    }

    public Repository(Path rootPath, Branch HEAD, HashSet<Branch> branches) {
        this.rootPath = rootPath;
        this.HEAD = HEAD;
        this.branches = branches;
    }

    public Branch getHEAD() {
        return this.HEAD;
    }

    public Branch getBranchByName(String branchName) throws NullPointerException {
        Branch out = null;
        for(Branch branch : branches) {
            if (branch.getName().equals(branchName) && !branch.getCollaborationSource().equals(CollaborationSource.REMOTE)){
                out = branch;
            }
         }

        if (out == null) {
            throw new NullPointerException("There is no branch named '" + branchName + "' in the repository.");
        }

        return out;
    }

    public Commit getLatestCommit() throws ParseException {
        Commit latestCommit = this.HEAD.getCommit();
        for (Branch branch : this.branches) {
            if (branch != HEAD) {
                if (Manager.getDateFromFormattedDateString(latestCommit.getDateCreated()).compareTo(Manager.getDateFromFormattedDateString(branch.getCommit().getDateCreated())) > 0) {
                    latestCommit = branch.getCommit();
                }
            }
        }
        return latestCommit;
    }

    public Branch getBranchByNameAll(String branchName) throws NullPointerException {
        Branch out = null;
        for(Branch branch : branches) {
            if (branch.getName().equals(branchName)){
                out = branch;
            }
        }

        if (out == null) {
            throw new NullPointerException("There is no branch named '" + branchName + "' in the repository.");
        }

        return out;
    }

    public void removeBranchByName(String branchName) throws NullPointerException {
        Branch branchToRemove = this.getBranchByName(branchName);
        if (branchToRemove == null) {
            throw new NullPointerException("There is no branch named '" + branchName + "' in the repository.");
        }
        this.branches.remove(branchToRemove);
    }

    public void swichHEAD(Branch newHead)
    {
        this.HEAD = newHead;
    }

    public String getName() {
        String [] path = rootPath.toString().split("/?\\\\");         //("[/\]");  Pattern.quote(separator)
        return path[path.length - 1];
    }

    public Path getRootPath() {
        return this.rootPath;
    }

    public Path getRemotePath() { return this.remotePath; }

    public HashSet<Branch> getBranches () {
        return this.branches;
    }

    public void setRootPath(Path rootPath) {
        this.rootPath = rootPath;
    }

    public CollaborationSource getCollaborationSource() { return this.collaborationSource; }

    public void setCollaborationSource(CollaborationSource collaborationSource) { this.collaborationSource = collaborationSource; }

    public void setBranches(HashSet<Branch> branches) {
        this.branches = branches;
    }

    public void setHEAD(Branch HEAD) {
        this.HEAD = HEAD;
    }

    public void setRemotePath(Path remotePath) { this.remotePath = remotePath; }
}
