package Engine;

import java.io.File;
import java.io.IOException;

public class Blob implements FolderComponent {
    private String content;

    public void exportToFile(){}

    public Blob(String content) {
        this.content = content;
    }

    public Blob(File file) throws IOException {
        this.content = Manager.readFileToString(file);
    }

    public String getContent() {
        return this.content;
    }
}
