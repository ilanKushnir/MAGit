package Engine;

import java.io.File;
import java.nio.file.Path;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedList;

public class Commit {
    private String parentCommitSHA = "";
    private String grandparentCommitSHA = "";
    private String description;
    private String dateCreated;
    private String author;
    private Folder tree;

    public Commit(Commit parentCommit, String author, String description, Folder tree) {
        this.dateCreated = Manager.getCurrentDateString();
        this.description = description;
        this.author = author;

        if (parentCommit != null) {
            this.parentCommitSHA = parentCommit.generateSHA();

            if (!parentCommit.getParentCommitSHA().equals("")) {
                this.grandparentCommitSHA = parentCommit.getParentCommitSHA();
            }
        }

        this.tree = tree;
    }

    public Folder getTree() {
        return this.tree;
    }

    public String getParentCommitSHA() {
        return this.parentCommitSHA;
    }

    public String getGrandparentCommitSHA() {
        return this.grandparentCommitSHA;
    }

    public String generateCommitFileContent() {
        String delimiter = ", ";
        StringBuilder sb = new StringBuilder();

        sb.append(this.parentCommitSHA);
        sb.append(System.lineSeparator());
        sb.append(this.grandparentCommitSHA);
        sb.append(System.lineSeparator());
        sb.append(this.description);
        sb.append(System.lineSeparator());
        sb.append(this.dateCreated);
        sb.append(System.lineSeparator());
        sb.append(this.author);
        sb.append(this.tree.generateSHA());

        return sb.toString();
    }

    public String generateSHA() {
        return Manager.generateSHA1FromString(this.generateCommitFileContent());
    }

    public Folder getRootFolder() {
        return this.tree;
    }

    // שתי הרשימות מלאות, רשימה חדשה מלאה, רשימה ישנה מלאה, שתי הרשימות ריקות
    public static StatusLog compareTrees(Folder recentTree, Folder wcTree, Path originalPath, Path path, Boolean shouldCommit) {
        StatusLog log = new StatusLog();
        Path componentPath;
        Iterator<Folder.Component> currTreeItr = wcTree.getComponents().iterator();
        Folder.Component wcComponent = currTreeItr.next();
        Folder.Component originalComponent = null;

        if (recentTree != null) {
            Iterator<Folder.Component> prevTreeItr = recentTree.getComponents().iterator();
            if(prevTreeItr.hasNext()) {
                originalComponent = prevTreeItr.next();
            int compareRes = 0;


            while (prevTreeItr.hasNext() || currTreeItr.hasNext()) {
//                while (prevTreeItr.hasNext() && currTreeItr.hasNext()) {
                compareRes = originalComponent.getName().compareTo(wcComponent.getName());

                try {
                    if (compareRes < 0) {   // the 'originalComponent' has been deleted from the repository
                        fileDeletedFromWC(prevTreeItr, originalComponent, originalPath, path, shouldCommit, log);
                    } else if (compareRes > 0) { // the 'wcComponent' is a newly added component to the repository
                        fileAddedToWC(currTreeItr, wcComponent, originalPath, path, shouldCommit, log);
                    } else { // compareRes == 0 --> components has the same name - check if same type and if SHA1 has been changed
                        if (!originalComponent.getType().equals(wcComponent.getType())) {
                            if (wcComponent.getType().equals(FolderType.FOLDER)) {
                                fileAddedToWC(currTreeItr, wcComponent, originalPath, path, shouldCommit, log);
                            } else { // if 'wcComponent' is a 'Blob' --> 'originalComponent' is a deleted 'Folder'
                                fileDeletedFromWC(prevTreeItr, originalComponent, originalPath, path, shouldCommit, log);
                            }
                            // if components has the same name
                        } else if (!originalComponent.getSHA().equals(wcComponent.getSHA())) {
                            fileUpdatedinWC(currTreeItr, prevTreeItr, originalComponent, wcComponent, originalPath, path, shouldCommit, log);
                        } else {  // if both components has equal name, type and SHA
                            wcComponent = originalComponent;
                            // insert the original component instead of the new 'wcComponent' to keep authenticity
                            if (currTreeItr.hasNext()) {
                                wcComponent = currTreeItr.next();
                            }
                            if (prevTreeItr.hasNext()) {
                                originalComponent = prevTreeItr.next();
                            }
                        }
                    }
                } catch (IOException e) { }

                try {   // in case one of the components list still has components
                    if (currTreeItr.hasNext()) {
                        fileAddedToWC(currTreeItr, wcComponent, originalPath, path, shouldCommit, log);
                    }
                    if (prevTreeItr.hasNext()) {
                        fileDeletedFromWC(prevTreeItr, originalComponent, originalPath, path, shouldCommit, log);
                    }
                } catch (IOException e) {
                }

            } // end of big while

            if (!prevTreeItr.hasNext() && !currTreeItr.hasNext()) { // in case of a single component for each tree

            }
        } else {    // recentTree == null
            while (currTreeItr.hasNext()) {
                try {
                    fileAddedToWC(currTreeItr, wcComponent, originalPath, path, shouldCommit, log);
                } catch (IOException e) {
                }
            }
        }

        return log;
    }

    public static void fileDeletedFromWC(Iterator<Folder.Component> prevTreeItr, Folder.Component originalComponent, Path originalPath, Path path, boolean shouldCommit, StatusLog log) throws IOException {
        Path componentPath = Paths.get(path.toString(), originalComponent.getName());
        log.addDeletedFilePath(componentPath);

        if (originalComponent.getType().equals(FolderType.FOLDER)) {
            compareTrees((Folder) originalComponent.getComponent(), null, originalPath, componentPath, shouldCommit);
        }

        if (prevTreeItr.hasNext()) {
            originalComponent = prevTreeItr.next();
        }
    }

    public static void fileAddedToWC(Iterator<Folder.Component> currTreeItr, Folder.Component wcComponent, Path originalPath, Path path, boolean shouldCommit, StatusLog log) throws IOException {
        Path componentPath = Paths.get(path.toString(), wcComponent.getName());
        log.addAddedFilePath(componentPath);

        if (shouldCommit) {
            // create new 'Folder' or 'Blob' file on directory
            Manager.createFileInMagit(wcComponent.getComponent(), originalPath);
        }

        if (wcComponent.getType().equals(FolderType.FOLDER)) {
            compareTrees(null, (Folder) wcComponent.getComponent(), originalPath, componentPath, shouldCommit);
        }

        if (currTreeItr.hasNext()) {
            wcComponent = currTreeItr.next();
        }
    }


    public static void fileUpdatedinWC(Iterator<Folder.Component> currTreeItr, Iterator<Folder.Component> prevTreeItr, Folder.Component originalComponent, Folder.Component wcComponent, Path originalPath, Path path, boolean shouldCommit, StatusLog log) throws IOException {
        Path componentPath = Paths.get(path.toString(), wcComponent.getName());
        log.addUpdatedFilePath(componentPath);

        if (shouldCommit) {
            // create new 'Blob' / 'Folder' file on directory
            Manager.createFileInMagit(wcComponent.getComponent(), path);
        }

        if (originalComponent.getType().equals(FolderType.FOLDER)) {
            compareTrees((Folder) originalComponent.getComponent(), (Folder) wcComponent.getComponent(), originalPath, componentPath, shouldCommit);
        }

        if (currTreeItr.hasNext()) {
            wcComponent = currTreeItr.next();
        }

        if (prevTreeItr.hasNext()) {
            originalComponent = prevTreeItr.next();
        }
    }
}