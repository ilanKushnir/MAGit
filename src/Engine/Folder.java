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

        public Component(String name, FolderType type, String lastModifier, String lastModified, FolderComponent component ) {
            this.name = name;
            this.type = type;
            this.lastModified = lastModified;
            this.lastModifier = lastModifier;
            this.component = component;
            this.SHA =
                    component instanceof Folder ?
                            ((Folder)component).generateSHA() :
                            Manager.generateSHA1FromString(((Blob)component).getContent());
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

        public String fullPathToString(Path path, int level) {
            String delimiter = ", ";
            StringBuilder sb = new StringBuilder();
            String indentation = generateIndentation(level);
            Path componentPath = Paths.get(path.toString(), name);


            sb.append(indentation).append("Path: ").append(componentPath.toString()).append(System.lineSeparator());
            sb.append(indentation).append("SHA1: ").append(SHA).append(System.lineSeparator());
            sb.append(indentation).append("Type: ").append(type.toString()).append(System.lineSeparator());
            sb.append(indentation).append("Last Modifier: ").append(lastModifier).append(System.lineSeparator());
            sb.append(indentation).append("Last Modified: ").append(lastModified).append(System.lineSeparator());

            if(component instanceof Folder) {
                sb.append(System.lineSeparator()).append(((Folder)component).showFolderContent(componentPath, level + 1));
            }
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

    public void addComponent(String name, FolderType type, String lastModifier, String lastModified, FolderComponent component ) {
        this.components.add(new Component(name, type, lastModifier, lastModified, component));
        sortComponentsList();
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

    public String showFolderContent(Path path, int level){
        StringBuilder sb = new StringBuilder();
        String indentation = generateIndentation(level);
        int counter = 0;
        sb.append(indentation).append("Folder Content: ").append(System.lineSeparator());

        for(Component component: components) {
            sb.append(indentation).append(++counter).append(System.lineSeparator());
            sb.append(component.fullPathToString(path, level)).append(System.lineSeparator());
        }

        return sb.toString();
    }

    public String generateIndentation(int level) {
        StringBuilder indentation = new StringBuilder();

        while(level > 0) {
            indentation.append("\t");
            level--;
        }

        return indentation.toString();
    }
}
