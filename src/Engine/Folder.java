package Engine;
import java.io.File;
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
            sb.append(lastModified + delimiter);

            return sb.toString();
        }
    } // end of 'Component' class

    public Folder() {
        this.components = new LinkedList<>();
    }

    public Folder(LinkedList<Component> componentsList) {
        this.components = componentsList;
    }

    public void setComponents(LinkedList<Component> components) {
        this.components = components;
    }

    public LinkedList<Component> getComponents() {
        return this.components;
    }

    //TODO delete last delimiter
    public String generateFolderContentString() {
        StringBuilder sb = new StringBuilder();

        for(Component c: components) {
            sb.append(c.toString());
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
}
