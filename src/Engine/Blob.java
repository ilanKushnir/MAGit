package Engine;

import java.io.File;

public class Blob implements FolderComponent {
    private String content;

    public void exportToFile(){}

    public Blob(String content) {
        this.content = content;
    }

    public Blob(File file) {
        this.content = Manager.readFileToString(file);
    }

    public String getContent() {
        return this.content;
    }
}
