package UI;

import Engine.Manager;

import java.io.IOException;
import java.nio.file.FileSystemNotFoundException;
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
        switch (choice) {
            case 5:
                commit();
                pressKeyToContinue("Commit finished.");
                break;
            case 12:
                createRepository();
                pressKeyToContinue("New repository created.");
                break;
            default:
                break;
        }
    }

    private void switchUser(){}

    private void importXML(){}

    private void switchRepository(){}

    private void showStatus(){
//        System.out.println(manager.showStatus());
    }

    private void commit(){
        System.out.println("Please enter commit message:" + System.lineSeparator());
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

    private void createRepository() {
        System.out.println("Enter path to create new repository:");
        Path path = Paths.get(getInputFromUser());

        System.out.println("Enter new repository name:");
        String repositoryName = getInputFromUser();

        try {
            manager.createNewRepository(path, repositoryName);
        }catch (FileSystemNotFoundException ex){
            System.out.println(ex.getMessage());
        }catch (Exception ex){
            System.out.println(ex.getMessage());
        }
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
