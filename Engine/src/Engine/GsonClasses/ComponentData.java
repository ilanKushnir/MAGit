package Engine.GsonClasses;

import Engine.Commons.FolderType;

public class ComponentData {
    private String name;
    private String content;
    private String type;
    private int level;

    public ComponentData(String name, String content, String type, int level) {
        this.name = name;
        this.content = content;
        this.type = type;
        this.level = level;
    }
}
