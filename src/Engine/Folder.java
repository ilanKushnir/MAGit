package Engine;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

public class Folder implements FolderComponent {
    private LinkedList<Component> components;

    public class Component {
        private String name;
        private String SHA;
        private FolderType type;
        private String lastModifier;
        private String lastModified;
        private FolderComponent component;

        public Component() {
            this.name = null;
            this.SHA = null;
            this.lastModified = null;
            this.lastModifier = null;
            this.type = null;
            this.component = null;
        }

        public Component(String componentString, Path objectsPath) throws FileNotFoundException, IOException {
            String[] componentStrings = componentString.split(", ");

            this.name = componentStrings[0];
            this.SHA = componentStrings[1];
            FolderType type = (componentStrings[2].equals("FOLDER"))? FolderType.FOLDER : FolderType.FILE;
            this.type = type;
            this.lastModifier = componentStrings[3];
            this.lastModified = componentStrings[4];

            File componentFile = new File(objectsPath + "//" + this.SHA + ".zip");
            if (!componentFile.exists())
                throw new FileNotFoundException("The '" + componentStrings[0] + "' " + componentStrings[2] +" wasn't found");

            if(type.equals(FolderType.FOLDER)) {
                this.component = new Folder(componentFile);
            } else {
                this.component = new Blob(componentFile);
            }
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setSHA(String SHA) {
            this.SHA = SHA;
        }

        public void setLastModified(String lastModified) {
            this.lastModified = lastModified;
        }

        public void setLastModifier(String lastModifier) {
            this.lastModifier = lastModifier;
        }

        public void setType(FolderType type) {
            this.type = type;
        }

        public void setComponent(FolderComponent component) {
            this.component = component;
        }

        public String getName() {
            return this.name;
        }

        public String getSHA() {
            return this.SHA;
        }

        public FolderType getType() {
            return this.type;
        }

        public String getLastModified() { return this.lastModified; }

        public FolderComponent getComponent() {
            return this.component;
        }

        @Override
        public String toString() {
            String delimiter = ", ";
            StringBuilder sb = new StringBuilder();

            sb.append(name + delimiter);
            sb.append(SHA + delimiter);
            sb.append(type.toString() + delimiter);
            sb.append(lastModifier + delimiter);
            sb.append(lastModified);

            return sb.toString();
        }

        public String fullPathToString(Path path) {
            String delimiter = ", ";
            StringBuilder sb = new StringBuilder();
            Path componentPath = Paths.get(path.toString(), name);

            sb.append(component + delimiter);
            sb.append(SHA + delimiter);
            sb.append(type.toString() + delimiter);
            sb.append(lastModifier + delimiter);
            sb.append(lastModified);
//TODO if component is a folder add its components to sb
            return sb.toString();
        }
    } // end of 'Component' class

    public Folder() {
        this.components = new LinkedList<>();
    }

    public Folder(LinkedList<Component> componentsList) {
        this.components = componentsList;
    }

    public Folder(File file) throws IOException {
        LinkedList<Component> components = new LinkedList<>();

        Path objectsPath = Paths.get(file.getParentFile().getPath());
        String fileContent = Manager.readFileToString(file);
        String componentStrings[] = fileContent.split("\\r?\\n");

        for (String compString : componentStrings) {
            components.add(new Component(compString, objectsPath));
        }

        this.components = components;
    }

    public void setComponents(LinkedList<Component> components) {
        this.components = components;
    }

    public LinkedList<Component> getComponents() {
        return this.components;
    }

    public String generateFolderContentString() {
        StringBuilder sb = new StringBuilder();

        for(Component comp: components) {
            sb.append(comp.toString());
            sb.append(System.lineSeparator());
        }

        return sb.toString();
    }

    public String generateSHA(){
        return Manager.generateSHA1FromString(this.generateFolderContentString());
    }

    public void addComponent(Component component) {
        if (component != null) {
            this.components.add(component);
            sortComponentsList();
        }
    }

    public void sortComponentsList() {
        Collections.sort(this.components, (comp1, comp2) -> {
            int res = comp1.getName().compareTo(comp2.getName());
            if (res == 0) {
                res = (comp1.getType().equals(FolderType.FOLDER))? -1 : 1;
            }
            return res;
        });
    }

    public String showFolderContent(Path path){
        StringBuilder sb = new StringBuilder();
        for(Component component: components) {
            sb.append(component.fullPathToString(path));
        }

        return sb.toString();
    }
}
