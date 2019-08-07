package Engine;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Iterator;

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

    public Commit(File commitFile) throws FileNotFoundException{
        Path objectsPath = Paths.get(commitFile.getParentFile().getPath());
        String fileContent = Manager.readFileToString(commitFile);
        String lines[] = fileContent.split("\\r?\\n");

        this.parentCommitSHA = lines[0];
        this.grandparentCommitSHA = lines[1];
        this.description = lines[2];
        this.dateCreated = lines[3];
        this.author = lines[4];

        File folderFile = new File(objectsPath + lines[5]);
        if(!folderFile.exists())
            throw new FileNotFoundException("One of the commits is pointing to a non existent tree");
        this.tree = new Folder(folderFile);
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
        sb.append(System.lineSeparator());
        sb.append(this.tree.generateSHA());

        return sb.toString();
    }

    public String generateSHA() {
        return Manager.generateSHA1FromString(this.generateCommitFileContent());
    }

    public Folder getRootFolder() {
        return this.tree;
    }

    // שתי הרשימות מלאות, רשימה חדשה מלאה, רשימה ישנה מלאה, שתי הרשימות ריקות(איבר בודד), רשימה ישנה ריקה (קומיט ריק)
    public static StatusLog compareTrees(Folder recentTree, Folder wcTree, Path originalPath, Path path, Boolean shouldCommit) {
        StatusLog log = new StatusLog();
        Path componentPath;
        Iterator<Folder.Component> currTreeItr = wcTree.getComponents().iterator();
        Folder.Component wcComponent = currTreeItr.next();
        Folder.Component originalComponent = null;

        if (recentTree != null) {
            Iterator<Folder.Component> prevTreeItr = recentTree.getComponents().iterator();

            if (prevTreeItr.hasNext()) {        // unnecessary??
                originalComponent = prevTreeItr.next();
                int compareRes = 0;


                while (prevTreeItr.hasNext() || currTreeItr.hasNext()) {
                        compareRes = originalComponent.getName().compareTo(wcComponent.getName());

                        try {
                            if (compareRes < 0) {   // the 'originalComponent' has been deleted from the repository (2 lists full)
                                originalComponent = fileDeletedFromWC(prevTreeItr, originalComponent, originalPath, path, shouldCommit, log);
                                if(!prevTreeItr.hasNext()) { // 'wcComponent' is a newly added component to the repository (prev list is empty)
                                    wcComponent = fileAddedToWC(currTreeItr, wcComponent, originalPath, path, shouldCommit, log);
                                }
                            } else if (compareRes > 0) { // the 'wcComponent' is a newly added component to the repository
                                wcComponent = fileAddedToWC(currTreeItr, wcComponent, originalPath, path, shouldCommit, log);
                                if(!currTreeItr.hasNext()) {    // the 'originalComponent' has been deleted from the repository (curr list empty)
                                    originalComponent = fileDeletedFromWC(prevTreeItr, originalComponent, originalPath, path, shouldCommit, log);
                                }
                            } else { // compareRes == 0 --> components has the same name - check if same type and if SHA1 has been changed
                                if (!originalComponent.getType().equals(wcComponent.getType())) {
                                    if (wcComponent.getType().equals(FolderType.FOLDER)) {
                                        wcComponent = fileAddedToWC(currTreeItr, wcComponent, originalPath, path, shouldCommit, log);
                                    } else { // if 'wcComponent' is a 'Blob' --> 'originalComponent' is a deleted 'Folder'
                                        originalComponent = fileDeletedFromWC(prevTreeItr, originalComponent, originalPath, path, shouldCommit, log);
                                    }
                                    // if components has the same name
                                } else if (!originalComponent.getSHA().equals(wcComponent.getSHA())) {
                                    wcComponent = fileUpdatedinWC(currTreeItr, prevTreeItr, originalComponent, wcComponent, originalPath, path, shouldCommit, log);
                                    if (prevTreeItr.hasNext()) {
                                        originalComponent = prevTreeItr.next();
                                    }
                                } else {  // if both components has equal name, type and SHA
                                    wcComponent = originalComponent;    // TODO might need to change only author and not the whole obj because of output parameter cant be changed
                                    // insert the original component instead of the new 'wcComponent' to keep authenticity
                                    if (currTreeItr.hasNext()) {
                                        wcComponent = currTreeItr.next();
                                    }
                                    if (prevTreeItr.hasNext()) {
                                        originalComponent = prevTreeItr.next();
                                    }
                                }
                            }
                        } catch (IOException e) {
                        }
                } // end of || while

                if (!prevTreeItr.hasNext() && !currTreeItr.hasNext()) { // in case of a single component for each tree
                    compareRes = originalComponent.getName().compareTo(wcComponent.getName());
                    try {
                        if (compareRes < 0) {   // the 'originalComponent' has been deleted from the repository
                            wcComponent = fileAddedToWC(currTreeItr, wcComponent, originalPath, path, shouldCommit, log);
                        } else if (compareRes > 0) { // the 'wcComponent' is a newly added component to the repository
                            originalComponent = fileDeletedFromWC(prevTreeItr, originalComponent, originalPath, path, shouldCommit, log);
                        } else { // compareRes == 0 --> components has the same name - check if same type and if SHA1 has been changed
                            if (!originalComponent.getType().equals(wcComponent.getType())) {
                                if (wcComponent.getType().equals(FolderType.FOLDER)) {
                                    fileAddedToWC(currTreeItr, wcComponent, originalPath, path, shouldCommit, log);
                                } else { // if 'wcComponent' is a 'Blob' --> 'originalComponent' is a deleted 'Folder'
                                    fileDeletedFromWC(prevTreeItr, originalComponent, originalPath, path, shouldCommit, log);
                                }
                                // if components has the same name
                            } else if (!originalComponent.getSHA().equals(wcComponent.getSHA())) {
                                wcComponent = fileUpdatedinWC(currTreeItr, prevTreeItr, originalComponent, wcComponent, originalPath, path, shouldCommit, log);
                                if (prevTreeItr.hasNext()) {
                                    originalComponent = prevTreeItr.next();
                                }
                            } else {  // if both components has equal name, type and SHA
                                wcComponent = originalComponent;
                                // insert the original component instead of the new 'wcComponent' to keep authenticity
                            }
                        }
                    } catch (IOException e) {
                    }
                }

            } else {    // prevTree has no components
                try{
                    while (currTreeItr.hasNext()) {
                        wcComponent = fileAddedToWC(currTreeItr, wcComponent, originalPath, path, shouldCommit, log);
                    }
                    wcComponent = fileAddedToWC(currTreeItr, wcComponent, originalPath, path, shouldCommit, log); // for last component
                } catch (IOException e) {
                }
            }

            } else {    // recentTree == null
            try{
                while (currTreeItr.hasNext()) {
                        wcComponent = fileAddedToWC(currTreeItr, wcComponent, originalPath, path, shouldCommit, log);
                    }
                wcComponent = fileAddedToWC(currTreeItr, wcComponent, originalPath, path, shouldCommit, log); // for last component
            } catch (IOException e) {
                }
            }
        return log;
    }

    public static void allFilesNew (Iterator<Folder.Component> prevTreeItr, Folder.Component originalComponent, Path originalPath, Path path, boolean shouldCommit, StatusLog log) throws IOException {

    }

    public static Folder.Component fileDeletedFromWC(Iterator<Folder.Component> prevTreeItr, Folder.Component originalComponent, Path originalPath, Path path, boolean shouldCommit, StatusLog log) throws IOException {
        Path componentPath = Paths.get(path.toString(), originalComponent.getName());
        log.addDeletedFilePath(componentPath);

        if (originalComponent.getType().equals(FolderType.FOLDER)) {
            log.mergeLogs(compareTrees((Folder) originalComponent.getComponent(), null, originalPath, componentPath, shouldCommit));
        }

        if (prevTreeItr.hasNext()) {
            return prevTreeItr.next();
        }
        return originalComponent;
    }

    public static Folder.Component fileAddedToWC(Iterator<Folder.Component> currTreeItr, Folder.Component wcComponent, Path originalPath, Path path, boolean shouldCommit, StatusLog log) throws IOException {
        Path componentPath = Paths.get(path.toString(), wcComponent.getName());
        log.addAddedFilePath(componentPath);

        if (shouldCommit) {
            // create new 'Folder' or 'Blob' file on directory
            Manager.createFileInMagit(wcComponent.getComponent(), originalPath);
        }

        if (wcComponent.getType().equals(FolderType.FOLDER)) {
            log.mergeLogs(compareTrees(null, (Folder) wcComponent.getComponent(), originalPath, componentPath, shouldCommit));
        }

        if (currTreeItr.hasNext()) {
            return currTreeItr.next();
        }
        return wcComponent;
    }

    public static Folder.Component fileUpdatedinWC(Iterator<Folder.Component> currTreeItr, Iterator<Folder.Component> prevTreeItr, Folder.Component originalComponent, Folder.Component wcComponent, Path originalPath, Path path, boolean shouldCommit, StatusLog log) throws IOException {
        Path componentPath = Paths.get(path.toString(), wcComponent.getName());
        log.addUpdatedFilePath(componentPath);

        if (shouldCommit) {
            // create new 'Blob' / 'Folder' file on directory
            Manager.createFileInMagit(wcComponent.getComponent(), originalPath);
        }

        if (originalComponent.getType().equals(FolderType.FOLDER)) {
            log.mergeLogs(compareTrees((Folder) originalComponent.getComponent(), (Folder) wcComponent.getComponent(), originalPath, componentPath, shouldCommit));
        }

        if (currTreeItr.hasNext()) {
            return currTreeItr.next();
        }

        return wcComponent;
    }
}