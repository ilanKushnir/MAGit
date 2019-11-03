package Engine.GsonClasses;

import Engine.Commons.FolderType;
import jdk.nashorn.internal.runtime.regexp.joni.Regex;

import java.io.File;

public class ComponentData {
    private String name;
    private String content;
    private String type;
    private int level;
    private String path = "";

    public ComponentData(String name, String content, String type, int level) {
        this.name = name;
        this.content = fixContent(content);
        this.type = type;
        this.level = level;
    }

    public ComponentData(String name, String content, String type, int level, String path) {
        this(name, content, type, level);
        this.path = replaceBackslashWithDoubleBackslahs(path + File.separator + name);
    }

    private String replaceBackslashWithDoubleBackslahs(String path) {
        return path.replace("\\", "\\\\");
    }

    private String fixContent(String content) {
        content.replaceAll("\'", "\\\\\'");
        content.replaceAll("\"", "\\\\\'");

        content.replaceAll("\'", "\\\\'");
        content.replaceAll("\"", "\\\\'");
//        Regex reg = new Regex("\"");
//        content.replaceAll(reg, "\\'");
//        s.replaceAll("'", "\\\\'");

//        .replaceAll("\\\\\"", "\"")

        return content;
    }
}