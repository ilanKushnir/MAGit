package UI;

import Engine.Manager;
import org.omg.PortableServer.POAPackage.ObjectAlreadyActive;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.modelmbean.XMLParseException;
import java.io.FileNotFoundException;
import java.io.IOException;
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
                case 2:
                    importFromXML();
                    endMessage = "XML file imported sucessfuly";
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
                case 6:
                    showActiveBranchhistory();
                    endMessage = "End of branch History.";
                    break;
                case 7:
                    showCommitInfo();
                    endMessage = "";
                    break;
                case 9:
                    endMessage = "Branch '" + newBranch() + "' created successfuly." + System.lineSeparator() +
                                 "The active branch is: '" + manager.getActiveRepository().getHEAD().getName() + "'";
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

    private void importFromXML() throws Exception {
        System.out.println("Please enter XML file path:");
        Path path = Paths.get(getInputFromUser());
        try {
            manager.importFromXML(path, false);
        } catch (ObjectAlreadyActive e) {
            System.out.println("Do you want to overwrite the existing repository? (Y/N):");
            String userCoice = getInputFromUser();
            if (userCoice.toLowerCase().equals("y")){
                manager.importFromXML(path, true);
            }
        }
    }

    private void switchRepository () throws IOException {
        System.out.println("Please enter a repository path:");
        Path path = Paths.get(getInputFromUser());
        // TODO debug this function
        manager.switchRepository(path);
    }

    private void showStatus() throws NullPointerException, IOException{
        String openChanges = "Open Changes on Working Copy:";
        StringBuilder sb = new StringBuilder();
        sb.append("Active Repository: ").append(manager.getActiveRepository().getName());
        sb.append(System.lineSeparator());
        sb.append("Path: ").append(manager.getActiveRepository().getRootPath());
        sb.append(System.lineSeparator());
        sb.append("Active User: ").append(manager.getActiveUser());
        sb.append(System.lineSeparator()).append(System.lineSeparator());
        sb.append(openChanges);
        sb.append(System.lineSeparator());
        sb.append(generateDivider(openChanges.length()));
        sb.append(System.lineSeparator());
        System.out.println(sb);
        System.out.println(manager.showStatus());
    }

    private void commit() throws IOException, NullPointerException {
        System.out.println("Please enter commit message:");
        String commitMessage = getInputFromUser();
        System.out.println(manager.commit(commitMessage));
    }

    private void showActiveBranchhistory() throws FileNotFoundException {
        System.out.println("The active branch is: " + manager.getActiveRepository().getHEAD().getName());
        if (manager.getActiveRepository().getHEAD().getCommit() == null) {
            System.out.println("There are no commits yet");
        } else {
            System.out.println("Commits history:" + System.lineSeparator());
            System.out.println(manager.generateActiveBranchHistory());
        }
    }

    private void showCommitInfo(){
        System.out.println(manager.showCommitInfo());
    }

    private void showBranches(){}

    private String newBranch() throws InstanceAlreadyExistsException, IOException {
        System.out.println("Please enter branch name:");
        String newBranchName = getInputFromUser();
        manager.createNewBranch(newBranchName);
        return newBranchName;
    }

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