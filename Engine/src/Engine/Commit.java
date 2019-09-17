package Engine;

import Engine.Commons.FolderType;
import puk.team.course.magit.ancestor.finder.CommitRepresentative;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Iterator;

public class Commit implements CommitRepresentative {
    private String parentCommitSHA = "";
    private String otherParentCommitSHA = "";
    private String description;
    private String dateCreated;
    private String author;
    private Folder tree;

    @Override
    public String getFirstPrecedingSha1() { return getParentCommitSHA();
    }

    @Override
    public String getSecondPrecedingSha1() { return getotherParentCommitSHA();
    }

    @Override
    public String getSha1() { return generateSHA();
    }

    public Commit(Commit parentCommit, String author, String description, Folder tree) {
        this.dateCreated = Manager.getCurrentDateString();
        this.description = description;
        this.author = author;

        if (parentCommit != null) {
            this.parentCommitSHA = parentCommit.generateSHA();
        }

        this.tree = tree;
    }

    public Commit(Commit parentCommit, Commit otherParentCommit, String author, String description, Folder tree){
        this(parentCommit, author, description, tree);

        if (otherParentCommit != null) {
            this.otherParentCommitSHA = otherParentCommit.generateSHA();
        }
    }

    public Commit(Commit parentCommit, String author, String description, String creationDate, Folder tree) {
        this(parentCommit, author, description, tree);
        this.dateCreated = creationDate;
    }


    public Commit(File commitFile) throws FileNotFoundException, IOException {
        Path objectsPath = Paths.get(commitFile.getParentFile().getPath());
        String fileContent = Manager.readFileToString(commitFile);
        String lines[] = fileContent.split("\\r?\\n");

        this.parentCommitSHA = lines[0];
        this.otherParentCommitSHA = lines[1];
        this.description = lines[2];
        this.dateCreated = lines[3];
        this.author = lines[4];

        File folderFile = new File(objectsPath + File.separator + lines[5] + ".zip");
        if(!folderFile.exists())
            throw new FileNotFoundException("One of the commits is pointing to a non existent tree");
        this.tree = new Folder(folderFile);
    }

    public String showCommitInfo(Path path) {
        StringBuilder sb = new StringBuilder();
        int level = 1;
        sb.append("Root Folder: ").append(path.toString()).append(System.lineSeparator()).append(System.lineSeparator());
        sb.append(tree.showFolderContent(path, level));
        return sb.toString();
    }

    public Folder getTree() {
        return this.tree;
    }

    public String getParentCommitSHA() {
        return this.parentCommitSHA;
    }

    public String getotherParentCommitSHA() {
        return this.otherParentCommitSHA;
    }

    public String generateCommitFileContent() {
        String delimiter = ", ";
        StringBuilder sb = new StringBuilder();

        sb.append(this.parentCommitSHA);
        sb.append(System.lineSeparator());
        sb.append(this.otherParentCommitSHA);
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

    public String getAuthor() {return this.author;}

    public static StatusLog compareTrees(Folder recentTree, Folder wcTree, Path originalPath, Path path, Boolean shouldCommit) {
        /* conditions :
            1. recentTree null:
                1.1 wcTree null
                1.2 wcTree single / last component
                1.3 wcTree full
            2. recentTree single / last component:
                2.1 wcTree null
                2.2 wcTree single / last component
                2.3 wcTree full
         */

        StatusLog log = new StatusLog();
        Path componentPath;
        Iterator<Folder.Component> currTreeItr = null;
        Folder.Component wcComponent = null;
        Folder.Component originalComponent = null;
        Boolean isChanged = false;

        try {

            if (recentTree != null) {
                Iterator<Folder.Component> prevTreeItr = recentTree.getComponents().iterator();

                if (prevTreeItr.hasNext()) {
                    originalComponent = prevTreeItr.next();
                    int compareRes = 0;

                    if (wcTree != null && wcTree.getComponents().iterator().hasNext()) {
                        currTreeItr = wcTree.getComponents().iterator();
                        wcComponent = currTreeItr.next();


                        while (prevTreeItr.hasNext() || currTreeItr.hasNext()) {
                            compareRes = originalComponent.getName().compareTo(wcComponent.getName());

                            if (compareRes < 0) {   // condition #2.3 - the 'originalComponent' has been deleted from the repository (2 lists full)
                                if (prevTreeItr.hasNext()) {
                                    originalComponent = fileDeletedFromWC(prevTreeItr, originalComponent, originalPath, path, shouldCommit, log);
                                } else {    // condition #2.1 -'wcComponent' is a newly added component to the repository (prev list is empty)
                                    wcComponent = fileAddedToWC(currTreeItr, wcComponent, originalPath, path, shouldCommit, log);
                                }
                            } else if (compareRes > 0) { // condition #2.3 - the 'wcComponent' is a newly added component to the repository
                                if (currTreeItr.hasNext()) {
                                    wcComponent = fileAddedToWC(currTreeItr, wcComponent, originalPath, path, shouldCommit, log);
                                } else {    // condition #2.1 - the 'originalComponent' has been deleted from the repository (curr list empty)
                                    originalComponent = fileDeletedFromWC(prevTreeItr, originalComponent, originalPath, path, shouldCommit, log);
                                }
                            } else { // condition #2.3 - compareRes == 0 --> components has the same name - check if same type and if SHA1 has been changed
                                if (!originalComponent.getType().equals(wcComponent.getType())) {
                                    if (wcComponent.getType().equals(FolderType.FOLDER)) {
                                        wcComponent = fileAddedToWC(currTreeItr, wcComponent, originalPath, path, shouldCommit, log);
                                    } else { // if 'wcComponent' is a 'Blob' --> 'originalComponent' is a deleted 'Folder'
                                        originalComponent = fileDeletedFromWC(prevTreeItr, originalComponent, originalPath, path, shouldCommit, log);
                                    }
                                    // if components has the same name and type
                                } else if (!originalComponent.getSHA().equals(wcComponent.getSHA())) {

                                    componentPath = Paths.get(path.toString(), wcComponent.getName());

                                    if (originalComponent.getType().equals(FolderType.FOLDER)) {
                                        boolean logUpdated;
                                        logUpdated = log.mergeLogs(compareTrees((Folder) originalComponent.getComponent(), (Folder) wcComponent.getComponent(), originalPath, componentPath, shouldCommit));
                                        if (!logUpdated) {
                                            wcComponent.setSHA(originalComponent.getSHA());
                                        }
                                    }

                                    // if both components has the same SHA1 after recursive call - those are tha same unchanged Folder
                                    if(wcComponent.getType().equals(FolderType.FOLDER) &&
                                            compareWithoutModifier(originalComponent, wcComponent) == true) {
                                        wcComponent.setLastModifier(originalComponent.getLastModifier());
                                    } else {    // 'component' has been changed
                                        log.addUpdatedFilePath(componentPath);
                                        if (shouldCommit) {
                                        // create new 'Blob' / 'Folder' file on directory
                                        Manager.createFileInMagit(wcComponent.getComponent(), originalPath);
                                        }
                                    }


                                    if (currTreeItr.hasNext()) {
                                            wcComponent =  currTreeItr.next();
                                    } if (prevTreeItr.hasNext()) {
                                        originalComponent = prevTreeItr.next();
                                    }
                                } else {  // if both components has equal name, type and SHA
                                    wcComponent.setLastModifier(originalComponent.getLastModifier());
                                    // insert the original component instead of the new 'wcComponent' to keep authenticity
                                    if (currTreeItr.hasNext()) {
                                        wcComponent = currTreeItr.next();
                                    }
                                    if (prevTreeItr.hasNext()) {
                                        originalComponent = prevTreeItr.next();
                                    }
                                }
                            }
                        } // end of || while

                        if (!prevTreeItr.hasNext() && !currTreeItr.hasNext()) { // condition #2.2 - in case of a single component for each tree
                            compareRes = originalComponent.getName().compareTo(wcComponent.getName());
                            if (compareRes < 0) {   // the 'wcComponent' is a newly added component to the repository
                                wcComponent = fileAddedToWC(currTreeItr, wcComponent, originalPath, path, shouldCommit, log);
                            } else if (compareRes > 0) { // the 'originalComponent' has been deleted from the repository
                                originalComponent = fileDeletedFromWC(prevTreeItr, originalComponent, originalPath, path, shouldCommit, log);
                            } else { // compareRes == 0 --> components has the same name - check if same type and if SHA1 has been changed
                                if (!originalComponent.getType().equals(wcComponent.getType())) {
                                    if (wcComponent.getType().equals(FolderType.FOLDER)) {
                                        fileAddedToWC(currTreeItr, wcComponent, originalPath, path, shouldCommit, log);
                                    } else { // if 'wcComponent' is a 'Blob' --> 'originalComponent' is a deleted 'Folder'
                                        fileDeletedFromWC(prevTreeItr, originalComponent, originalPath, path, shouldCommit, log);
                                    }
                                    // if components has the same name and type
                                } else if (!originalComponent.getSHA().equals(wcComponent.getSHA())) {
                                    componentPath = Paths.get(path.toString(), wcComponent.getName());

                                    if (originalComponent.getType().equals(FolderType.FOLDER)) {
                                        boolean logUpdated;
                                        logUpdated = log.mergeLogs(compareTrees((Folder) originalComponent.getComponent(), (Folder) wcComponent.getComponent(), originalPath, componentPath, shouldCommit));
                                        if (!logUpdated) {
                                            wcComponent.setSHA(originalComponent.getSHA());
                                        }
                                    }

                                    // if both components has the same SHA1 after recursive call - those are tha same unchanged Folder
                                    if(wcComponent.getType().equals(FolderType.FOLDER) &&
                                            compareWithoutModifier(originalComponent, wcComponent) == true) {
                                        wcComponent.setLastModifier(originalComponent.getLastModifier());
                                    } else {    // 'component' has been changed
                                        log.addUpdatedFilePath(componentPath);
                                        if (shouldCommit) {
                                            // create new 'Blob' / 'Folder' file on directory
                                            Manager.createFileInMagit(wcComponent.getComponent(), originalPath);
                                        }
                                    }

                                    if (currTreeItr.hasNext()) {
                                        wcComponent =  currTreeItr.next();
                                    } if (prevTreeItr.hasNext()) {
                                        originalComponent = prevTreeItr.next();
                                    }
                                } else {  // if both components has equal name, type and SHA
                                    wcComponent.setLastModifier(originalComponent.getLastModifier());
                                    // insert the original component instead of the new 'wcComponent' to keep authenticity
                                }
                            }
                        }
                    } else { // condition #2.1 - wcTree == null - (condition: Working Copy is Empty)
                        while (prevTreeItr.hasNext()) {
                            originalComponent = fileDeletedFromWC(prevTreeItr, originalComponent, originalPath, path, shouldCommit, log);
                        }
                        originalComponent = fileDeletedFromWC(prevTreeItr, originalComponent, originalPath, path, shouldCommit, log);
                    }
                } else {    // condition #1.3 - recentTree has no components - (condition: 1st commit holds an empty 'tree')
                    wcComponent = recentTreeIsEmpty(wcTree, originalPath, path, shouldCommit, log);
                }

            } else {    // condition #1.3 - recentTree == null - (condition: no previous commits)
                wcComponent = recentTreeIsEmpty(wcTree, originalPath, path, shouldCommit, log);
            } // condition # 1.1 - recentTree & wcTree is empty
        }catch (IOException e) {}

        return log;
    }

    public static boolean compareWithoutModifier(Folder.Component originalComponent, Folder.Component wcComponent) {
        String wcModifier = wcComponent.getLastModifier();
        wcComponent.setLastModifier(originalComponent.getLastModifier());
        Boolean isEqualSHA = ((Folder)wcComponent.getComponent()).generateSHA().equals(((Folder)originalComponent.getComponent()).generateSHA());
        wcComponent.setLastModifier(wcModifier);
        return isEqualSHA;
        }

    public static Folder.Component recentTreeIsEmpty (Folder wcTree, Path originalPath, Path path, boolean shouldCommit, StatusLog log) throws IOException {
        if(wcTree != null ) {
            Iterator<Folder.Component> currTreeItr = wcTree.getComponents().iterator();
            Folder.Component wcComponent = currTreeItr.next();

            while (currTreeItr.hasNext()) {
                wcComponent = fileAddedToWC(currTreeItr, wcComponent, originalPath, path, shouldCommit, log);
            }
            return fileAddedToWC(currTreeItr, wcComponent, originalPath, path, shouldCommit, log); // for last component
        }
        return null;
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