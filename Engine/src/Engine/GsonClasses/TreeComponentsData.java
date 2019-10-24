package Engine.GsonClasses;

import Engine.Blob;
import Engine.Commons.FolderType;
import Engine.Folder;
import Engine.FolderComponent;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class TreeComponentsData {
    private List<ComponentData> components;

    public TreeComponentsData(Folder tree) {
        addComponentsToListRec(0, tree);
    }

    private void addComponentsToListRec(int level, Folder tree) {
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
            components.add(new ComponentData(name, "", "folder", level));
            addComponentsToListRec(level + 1, foldersMap.get(name));
        }
        for(String name : filesMap.keySet()) {
            String fileContent = filesMap.get(name).getContent();
            components.add(new ComponentData(name, fileContent, "file", level));
        }
    }

    public List<ComponentData> getComponents() {
        return components;
    }
}
