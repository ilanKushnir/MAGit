package UI;

import Engine.Manager;
import org.omg.PortableServer.POAPackage.ObjectAlreadyActive;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
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
        Boolean succeeded;

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
                case 4:
                    showStatus();
                    endMessage = "";
                    break;
                case 5:
                    commit();
                    endMessage = "Commit finished.";
                    break;
                case 11:
                    checkout();
                    endMessage = "Checked out to '" +
                                 this.manager.getActiveRepository().getHEAD().getName() +
                                 "' branch.";
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
        // TODO debug this function
        manager.switchRepository(path);
    }

    private void showStatus() throws NullPointerException, IOException{
        System.out.println(manager.showStatus());
    }

    private void commit() throws IOException, NullPointerException {
        System.out.println("Please enter commit message:");
        String commitMessage = getInputFromUser();
        System.out.println(manager.commit(commitMessage));
    }

    private void showActiveBranchInfo(){}

    private void showCommitInfo(){}

    private void showBranches(){}

    private void newBranch(){}

    private void deleteBranch(){}

    private void checkout() throws IOException, ParseException, ObjectAlreadyActive {
        // TODO test checkout
        if(!manager.showStatus().isEmptyLog()) {
            System.out.println("There are uncommited changes, choose action:");
            System.out.println("1) Commit before checking out");
            System.out.println("2) Checkout anyway (will cause lose of information)");
            System.out.println("3) Abort");
            String userChoice = getInputFromUser();
            if(userChoice.equals("1")) {
                this.commit();
                System.out.println("All changes are commited and you can checkout.");
            } else if(userChoice.equals("2")) {
                System.out.println("Checking out... please enter a branch name:");
                String checkoutBranch = getInputFromUser();
                this.manager.checkout(checkoutBranch);
            } else if(userChoice.equals("3")) {
                System.out.println("Checkout aborted");
            } else {
                System.out.println("Wrong input, checkout aborted");
            }
        } else {
            System.out.println("Checking out... please enter a branch name:");
            String checkoutBranch = getInputFromUser();
            this.manager.checkout(checkoutBranch);
        }
    }

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
