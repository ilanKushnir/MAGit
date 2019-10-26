package Engine;

import Engine.GsonClasses.RepositoryData;
import Engine.GsonClasses.UserData;
import Engine.Commons.Constants;
import org.omg.PortableServer.POAPackage.ObjectAlreadyActive;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.*;

public class MAGitHubManager {

    private HashMap<String, User> users;

    public MAGitHubManager() {
        users = new HashMap<>();
    }

    public synchronized void addUser(String username) {
        if (!users.containsKey(username)) {
            users.put(username, new User(username));
            new File(Constants.MAGITHUB_FOLDER_PATH + File.separator + username).mkdirs();
        }
    }

    public synchronized User getUser(String username) {
        return users.get(username);
    }

    public synchronized void removeUser(String username) {
        users.remove(username);
    }

    public synchronized HashMap<String, User> getUsers() {
        return (HashMap<String, User>) Collections.unmodifiableMap(users);
    }

    public boolean isUserExists(String username) {
        return users.containsKey(username);
    }

    public List<UserData> GetOtherUsersData(String currentUserName) {
        List<UserData> otherUsersData = new ArrayList<>();

        UserData userDataToAdd;
        File usersDirectory = new File(Constants.MAGITHUB_FOLDER_PATH);

        for (File file : usersDirectory.listFiles()) {
            userDataToAdd = getUserDataFromFile(file.getName());
            if (!file.getName().equals(currentUserName)) {
                otherUsersData.add(userDataToAdd);
            }
        }
        return otherUsersData;
    }

    private UserData getUserDataFromFile(String userName) {
        UserData userData = new UserData(userName);
        File userDirectory = new File(Constants.MAGITHUB_FOLDER_PATH + File.separator + userName);
        for (File file : userDirectory.listFiles()) {
            addRepositoryDirectoryToUserData(userData, file, userName);
        }
        return userData;
    }

    public UserData GetUserDataObj(String currentUserName) {
        UserData currentUserData;
        String currentUserDirectoryPath = Constants.MAGITHUB_FOLDER_PATH + File.separator + currentUserName;
        if (!Files.exists(Paths.get(currentUserDirectoryPath))) {
            return null;
        }
        else {
            currentUserData = getUserDataFromFile(currentUserName);
            return currentUserData;
        }
    }

    private void addRepositoryDirectoryToUserData(UserData userData, File directoryFile, String userName) {
        RepositoryData repositoryData;
        String name;
        Integer numberOfBranches;
        String activeBranchName = null;
        String lastCommitDate = null;
        String lastCommitMessage = null;
        String lastCommitSha1;
        String repositoryPath = Constants.MAGITHUB_FOLDER_PATH + File.separator + userName + File.separator + directoryFile.getName();
        String repositoryBranchesPath = repositoryPath + File.separator + ".magit" + File.separator + "branches";

        name = directoryFile.getName();
        numberOfBranches = new File(repositoryBranchesPath).listFiles().length;

        try {
            activeBranchName = Manager.readFileToString(repositoryBranchesPath + File.separator + "HEAD");
            lastCommitSha1 = Manager.readFileToString(repositoryBranchesPath + File.separator + activeBranchName);

            Commit lastCommit = new Commit(new File(repositoryPath + File.separator + ".magit" + File.separator + "objects" + File.separator + lastCommitSha1 + ".zip"));
            lastCommitDate = lastCommit.getDateCreated();
            lastCommitMessage = lastCommit.getDescription();
        } catch (IOException e) {
            e.printStackTrace();
        }

        repositoryData = new RepositoryData(name, activeBranchName, numberOfBranches, lastCommitDate, lastCommitMessage);
        userData.AddRepositoryDataToRepositorysDataList(repositoryData);
    }

    public void switchActiveRepository(String userName, String repositoryName) throws IOException, ParseException {
        getUser(userName).getManager().SwitchRepositoryInHub(repositoryName);;
    }

    public void checkout(String userName, String branchToCheckout) throws FileNotFoundException, ParseException, ObjectAlreadyActive {
        getUser(userName).getManager().checkout(branchToCheckout);
    }

//    public void setActiveUser(String userName) {
//        this.activeUser = users.get(userName);
//    }
//
//    public Repository getActiveRepository() {
//        return this.activeRepository;
//    }

//    public User getActiveUser() {
//        return this.activeUser;
//    }
}