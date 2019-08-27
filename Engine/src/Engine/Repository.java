package Engine;

import java.nio.file.Path;
import java.util.HashSet;

public class Repository {

    private Path rootPath;
    private Branch HEAD;                    // pointer to active branch
    private HashSet<Branch> branches;

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

    public HashSet<Branch> getBranches () {
        return this.branches;
    }

    public void setRootPath(Path rootPath) {
        this.rootPath = rootPath;
    }

    public void setBranches(HashSet<Branch> branches) {
        this.branches = branches;
    }

    public void setHEAD(Branch HEAD) {
        this.HEAD = HEAD;
    }
}
