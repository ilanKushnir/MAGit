package Engine.GsonClasses;

import Engine.Commons.FolderType;

import java.io.File;

public class ComponentData {
    private String name;
    private String content;
    private String type;
    private int level;
    private String path = "";

    public ComponentData(String name, String content, String type, int level) {
        this.name = name;
        this.content = content;
        this.type = type;
        this.level = level;
    }

    public ComponentData(String name, String content, String type, int level, String path) {
        this(name, content, type, level);
        this.path = path + File.separator + name;
    }
}
