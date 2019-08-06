import Engine.Folder;
import Engine.Manager;
import UI.ConsoleUI;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {

        Manager manager = new Manager();
        ConsoleUI UI = new ConsoleUI(manager);
        UI.run();
    }
}
