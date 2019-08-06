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

    public static StatusLog compareTrees(Folder recentTree, Folder wcTree,Path originalPath, Path path, Boolean shouldCommit) {
        StatusLog log = new StatusLog();
        Path componentPath;
        Iterator<Folder.Component> currTreeItr = wcTree.getComponents().iterator();
        Folder.Component wcComponent = currTreeItr.next();

        if(recentTree != null) {
            Iterator<Folder.Component> prevTreeItr = recentTree.getComponents().iterator();
            Folder.Component originalComponent = prevTreeItr.next();

            int compareRes = 0;

            do {
                compareRes = originalComponent.getName().compareTo(wcComponent.getName());

                try {
                    if (compareRes < 0) {   // the 'originalComponent' has been deleted from the repository
                        log.addDeletedFilePath(Paths.get(path.toString(), originalComponent.getName()));
                        if(prevTreeItr.hasNext()) {
                            originalComponent = prevTreeItr.next();
                        }
                    } else if (compareRes > 0) { // the 'wcComponent' is a newly added component to the repository
                        fileAddedToWC(wcComponent, originalPath, path, shouldCommit, log);
                        if(currTreeItr.hasNext()) {
                            wcComponent = currTreeItr.next();
                        }
                    } else { // compareRes == 0 --> components has the same name - check if same type and if SHA1 has been changed
                        if (!originalComponent.getType().equals(wcComponent.getType())) {
                            if (wcComponent.getType().equals(FolderType.FOLDER)) {
                                fileAddedToWC(wcComponent, originalPath, path, shouldCommit, log);
                                if(currTreeItr.hasNext()) {
                                    wcComponent = currTreeItr.next();
                                }
                            } else { // if 'wcComponent' is a 'Blob' --> 'originalComponent' is a deleted 'Folder'
                                log.addDeletedFilePath(Paths.get(path.toString(), originalComponent.getName()));
                                if(prevTreeItr.hasNext()) {
                                    originalComponent = prevTreeItr.next();
                                }
                            }
                            // if components has the same name
                        } else if (!originalComponent.getSHA().equals(wcComponent.getSHA())) {
                            log.addUpdatedFilePath(Paths.get(path.toString(), wcComponent.getName()));

                            if (shouldCommit) {
                                // create new 'Blob' / 'Folder' file on directory
                                Manager.createFileInMagit(wcComponent.getComponent(), path);
                            }

                            if (originalComponent.getType().equals(FolderType.FOLDER)) {
                                componentPath = Paths.get(path.toString(), originalComponent.getName());
                                compareTrees((Folder) originalComponent.getComponent(), (Folder) wcComponent.getComponent(),originalPath, componentPath, shouldCommit);
                            }
                            if(currTreeItr.hasNext()) {
                                wcComponent = currTreeItr.next();
                            }
                            if(prevTreeItr.hasNext()) {
                                originalComponent = prevTreeItr.next();
                            }

                        } else {  // if both components has equal name, type and SHA
                            wcComponent = originalComponent;
                            // insert the original component instead of the new 'wcComponent' to keep authenticity
                            if(currTreeItr.hasNext()) {
                                wcComponent = currTreeItr.next();
                            }
                            if(prevTreeItr.hasNext()) {
                                originalComponent = prevTreeItr.next();
                            }
                        }
                    }
                } catch (IOException e) {}
            } while (prevTreeItr.hasNext() && currTreeItr.hasNext());

            try {   // in case one of the components list still has components
                if (currTreeItr.hasNext()) {
                    do {
                        fileAddedToWC(wcComponent, originalPath, path, shouldCommit, log);
                        if (currTreeItr.hasNext()) {
                            wcComponent = currTreeItr.next();
                        }
                    } while (currTreeItr.hasNext());
                }
                if (prevTreeItr.hasNext()) {
                    do {
                        log.addDeletedFilePath(Paths.get(path.toString(), originalComponent.getName()));
                        if (prevTreeItr.hasNext()) {
                            originalComponent = prevTreeItr.next();
                        }
                    } while (prevTreeItr.hasNext());

                }
            } catch (IOException e) {}
        } else {    // recentTree == null
            do {
                try {
                    fileAddedToWC(wcComponent, originalPath, path, shouldCommit,log);
                    if(currTreeItr.hasNext()) {
                        wcComponent = currTreeItr.next();
                    }
                } catch (IOException e) {}
            } while(currTreeItr.hasNext());     //// ???
        }

        return log;
    }

    public static StatusLog fileAddedToWC(Folder.Component wcComponent,Path originalPath, Path path, boolean shouldCommit, StatusLog log) throws IOException {
        Path componentPath = Paths.get(path.toString(),wcComponent.getName());
        log.addAddedFilePath(componentPath);

        if (shouldCommit) {
            // create new 'Folder' or 'Blob' file on directory
            Manager.createFileInMagit(wcComponent.getComponent(), originalPath);
        }

        if(wcComponent.getType().equals(FolderType.FOLDER)) {
            compareTrees(null, (Folder) wcComponent.getComponent(), originalPath, componentPath, shouldCommit);
        }

        return log;
    }
}