package UI;

import Engine.Manager;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class ConsoleUI {
    private Manager manager;

    public ConsoleUI(Manager manager){
        this.manager = manager;
    }

    public void run(){
        int choice;
        do {
            Menu.printMenu();
            Scanner scan = new Scanner(System.in);
            choice = scan.nextInt(); // EXCEPTIONNNNN
            runCommand(choice);
        } while(choice != 13);

        System.out.println("----- Thanks for using MAGit! -----");
    }

    private void runCommand(int choice){
        String endMessage = "";

        try {
            switch (choice) {
                case 1:
                    switchUser();
                    endMessage = "User switched to: " + this.manager.getActiveUser();
                    break;
                case 3:
                    switchRepository();
                    endMessage = "Repository switched successfuly.";
                    break;
                case 5:
                    commit();
                    endMessage = "Commit finished.";
                    break;
                case 12:
                    createRepository();
                    endMessage = "New repository created.";
                    break;
                default:
                    break;
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }

        pressKeyToContinue(endMessage);
    }

    private void switchUser(){
        System.out.println("Please enter a user name to switch to:");
        manager.switchUser(getInputFromUser());
    }

    private void importXML(){}

    private void switchRepository () throws IOException {
        System.out.println("Please enter a repository path:");
        Path path = Paths.get(getInputFromUser());
        manager.switchRepository(path);
    }

    private void showStatus(){
    }

    private void commit() throws IOException {
        System.out.println("Please enter commit message:");
        String commitMessage = getInputFromUser();
        System.out.println(manager.commit(commitMessage));
    }

    private void showActiveBranchInfo(){}

    private void showCommitInfo(){}

    private void showBranches(){}

    private void newBranch(){}

    private void deleteBranch(){}

    private void checkout(){}

    private void exit(){}

    private void createRepository() throws Exception {
        System.out.println("Enter path to create new repository:");
        Path path = Paths.get(getInputFromUser());

        System.out.println("Enter new repository name:");
        String repositoryName = getInputFromUser();

        manager.createNewRepository(path, repositoryName);
    }

    private String getInputFromUser(){
        Scanner scan = new Scanner(System.in);
        String userInput = scan.nextLine();
        return userInput;
    }

    private void pressKeyToContinue(String message) {
        System.out.println(generateDivider(message.length()));
        System.out.println(message);
        System.out.println(generateDivider(message.length()));
        System.out.println("Press any key to continue...");
        try{
            System.in.read();
        }
        catch(Exception e) {}
    }

    private String generateDivider(int length) {
        StringBuilder divider = new StringBuilder();
        for (int i=0; i<length; i++){
            divider.append("-");
        }
        return divider.toString();
    }

}
