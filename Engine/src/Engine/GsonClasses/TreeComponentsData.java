package Engine.GsonClasses;

import Engine.Blob;
import Engine.Commons.FolderType;
import Engine.Folder;
import Engine.FolderComponent;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class TreeComponentsData {
    private List<ComponentData> components = new LinkedList<>();

    public TreeComponentsData(Folder tree) {
        addComponentsToListRec(0, tree, null);
    }

    public TreeComponentsData(Folder tree, Path rootPath) {
        addComponentsToListRec(0, tree, rootPath.toString());
    }

    private void addComponentsToListRec(int level, Folder tree, String path) {
        HashMap<String, Folder> foldersMap = new HashMap<>();
        HashMap<String, Blob>  filesMap = new HashMap<>();

        for (Folder.Component component : tree.getComponents()) {
            if (component.getType().equals(FolderType.FOLDER)) {
                foldersMap.put(component.getName(), (Folder)component.getComponent());
            } else {
                filesMap.put(component.getName(), (Blob)component.getComponent());
            }
        }

        for(String name : foldersMap.keySet()) {
            ComponentData folderData = new ComponentData(name, "", "folder", level, path);
            this.components.add(folderData);
            addComponentsToListRec(level + 1, foldersMap.get(name), path + File.separator + name);
        }
        for(String name : filesMap.keySet()) {
            String fileContent = filesMap.get(name).getContent();
            ComponentData fileData = new ComponentData(name, fileContent, "file", level, path);
            this.components.add(fileData);
        }
    }

    public List<ComponentData> getComponents() {
        return components;
    }
}
