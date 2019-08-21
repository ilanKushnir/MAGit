package Engine;

import java.nio.file.Path;
import java.util.LinkedList;

public class StatusLog {
    private Path path;
    private LinkedList<Path> updatedFiles;
    private LinkedList<Path> addedFiles;
    private LinkedList<Path> deletedFiles;

    public StatusLog() {
        this.updatedFiles = new LinkedList<>();
        this.addedFiles = new LinkedList<>();
        this.deletedFiles = new LinkedList<>();
    }

    public void CheckChanges(FolderComponent component) {

    }

    public LinkedList<Path> getUpdatedFilesPaths() {
        return this.updatedFiles;
    }

    public LinkedList<Path> getAddedFilesPaths() {
        return this.addedFiles;
    }

    public LinkedList<Path> getDeletedFilesPaths() {
        return this.deletedFiles;
    }

    public void addUpdatedFilePath(Path path) {
        this.updatedFiles.add(path);
    }

    public void addAddedFilePath(Path path) {
        this.addedFiles.add(path);
    }
    public void addDeletedFilePath(Path path) {
        this.deletedFiles.add(path);
    }

    public boolean mergeLogs(StatusLog other) {
        int sizeUpdated = updatedFiles.size();
        int sizeAdded = addedFiles.size();
        int sizeDeleted = deletedFiles.size();

        this.updatedFiles.addAll(other.updatedFiles);
        this.addedFiles.addAll(other.addedFiles);
        this.deletedFiles.addAll(other.deletedFiles);

        return (
                sizeUpdated != updatedFiles.size() ||
                sizeAdded != addedFiles.size() ||
                sizeDeleted != deletedFiles.size()
                );
    }

    public boolean isEmptyLog() {
        return this.addedFiles.isEmpty() && this.deletedFiles.isEmpty() && this.updatedFiles.isEmpty();
    }

    @Override
    public String toString() {
        String separator = System.lineSeparator() + "--------------" + System.lineSeparator();
        StringBuilder sb = new StringBuilder();
        boolean notEmptyLog = false;

        if(!updatedFiles.isEmpty()) {
            sb.append("Updated files:");
            sb.append(separator);
            for (Path updatedFilePath : updatedFiles) {
                sb.append(updatedFilePath.toString());
                sb.append(System.lineSeparator());
            }
            sb.append(System.lineSeparator());
            notEmptyLog = true;
        }

        if(!addedFiles.isEmpty()) {
            sb.append("Added files:");
            sb.append(separator);
            for (Path addedFilePath : addedFiles) {
                sb.append(addedFilePath.toString());
                sb.append(System.lineSeparator());
            }
            sb.append(System.lineSeparator());
            notEmptyLog = true;
        }

        if(!deletedFiles.isEmpty()) {
            sb.append("Deleted files:");
            sb.append(separator);
            for (Path deletedFilePath : deletedFiles) {
                sb.append(deletedFilePath.toString());
                sb.append(System.lineSeparator());
            }
            sb.append(System.lineSeparator());
            notEmptyLog = true;
        }

        if(!notEmptyLog) {
            sb.append("No changes has been made on Working Copy").append(System.lineSeparator());
        }

        return sb.toString();
    }
}