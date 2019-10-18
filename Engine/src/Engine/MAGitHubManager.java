package Engine;

import Engine.GsonClasses.RepositoryData;
import Engine.GsonClasses.UserData;
import Engine.Commons.Constants;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
            activeBranchName = Manager.readFileToString(repositoryBranchesPath + File.separator + "HEAD.txt");
            lastCommitSha1 = Manager.readFileToString(repositoryBranchesPath + File.separator + activeBranchName + ".txt");

            Commit lastCommit = new Commit(new File(repositoryPath + File.separator + ".magit" + File.separator + "objects" + File.separator + lastCommitSha1));
            lastCommitDate = lastCommit.getDateCreated();
            lastCommitMessage = lastCommit.getDescription();
        } catch (IOException e) {
            e.printStackTrace();
        }

        repositoryData = new RepositoryData(name, activeBranchName, numberOfBranches, lastCommitDate, lastCommitMessage);
        userData.AddRepositoryDataToRepositorysDataList(repositoryData);
    }
}















//package Engine;
//
//import Engine.GsonClasses.RepositoryData;
//import Engine.GsonClasses.UserData;
//
//import java.io.File;
//import java.io.IOException;
//import java.nio.file.Paths;
//import java.text.ParseException;
//import java.util.*;
//
//public class MAGitHubManager {
//    private HashSet<User> users;
//    private Manager manager;
//    private final String MAGITHUB_FOLDER_PATH = "c:" + File.separator + "magit-ex3";
//
//    // TODO debug
//    public MAGitHubManager() {
//        this.users = new HashSet<>();
//        this.manager = new Manager();
//
//        File file = new File(MAGITHUB_FOLDER_PATH);
//        file.mkdir();
//    }
//
//
//    public synchronized void removeUser(User user) { users.remove(user); }
//    public synchronized Set<User> getUsers() {
//        return Collections.unmodifiableSet(users);
//    }
//    public boolean isUserExists(String userName) { return users.stream().anyMatch(userName::equals); }
//
//
//    ////////////////
//    // LOGIN PAGE //
//    ////////////////
//
//    public synchronized void addUser(User newUser) {
//        users.add(newUser);
//        new File(MAGITHUB_FOLDER_PATH + File.separator + newUser.getUserName()).mkdir();
//    }
//
//    public synchronized void addUser(String newUser) {
//        users.add(new User(newUser));
//        new File(MAGITHUB_FOLDER_PATH + File.separator + newUser).mkdir();
//    }
//
//    // TODO debug
//    public User getUserByName(String username) {
//        User out = null;
//
//        for(User userObj : users) {
//            if (userObj.getUserName().equals(username)) {
//                out = userObj;
//            }
//        }
//
//        return out;
//    }
//
//
//    /////////////////////
//    // BOTH REPO PAGES //
//    /////////////////////
//
//    // TODO debug
//    public void cloneOtherUsersRepository(String loggedInUsername, String otherUsername, String repoName) throws Exception {
//
//        manager.clone(Paths.get(MAGITHUB_FOLDER_PATH + File.separator + otherUsername + File.separator + repoName),
//                Paths.get(MAGITHUB_FOLDER_PATH + File.separator + loggedInUsername + File.separator + repoName));
//    }
//
//    public List<UserData> GetOtherUsersData(String currentUserName) {
//        List<UserData> otherUsersData = new ArrayList<>();
//
//        UserData userDataToAdd;
//        File usersDirectory = new File(MAGITHUB_FOLDER_PATH);
//
//        for (File file : usersDirectory.listFiles()) {
//            userDataToAdd = getUserData(file.getName());
//            if (!file.getName().equals(currentUserName)) {
//                otherUsersData.add(userDataToAdd);
//            }
//        }
//        return otherUsersData;
//    }
//
//    private UserData getUserData(String userName) {
//        UserData userData = new UserData(userName);
//        File userDirectory = new File(MAGITHUB_FOLDER_PATH + File.separator + userName);
//        for (File file : userDirectory.listFiles()) {
//            addRepositoryDirectoryToUserData(userData, file, userName);
//        }
//        return userData;
//    }
//
//    private void addRepositoryDirectoryToUserData(UserData userData, File directoryFile, String userName) {
//        RepositoryData repositoryData;
//        String name;
//        Integer numberOfBranches;
//        String activeBranchName = null;
//        String lastCommitDate = null;
//        String lastCommitMessage = null;
//        String lastCommitSha1;
//        String repositoryPath = MAGITHUB_FOLDER_PATH + File.separator + userName + File.separator + directoryFile.getName();
//        String repositoryBranchesPath = repositoryPath + File.separator + ".magit" + File.separator + "branches";
//
//        name = directoryFile.getName();
//        numberOfBranches = new File(repositoryBranchesPath).listFiles().length;
//
//        try {
//            activeBranchName = manager.readFileToString(repositoryBranchesPath + File.separator + "HEAD.txt");
//            lastCommitSha1 = manager.readFileToString(repositoryBranchesPath + File.separator + activeBranchName + ".txt");
//
//            Commit lastCommit = new Commit(new File(repositoryPath + File.separator + ".magit" + File.separator + "objects" + File.separator + lastCommitSha1));
//            lastCommitDate = lastCommit.getDateCreated();
//            lastCommitMessage = lastCommit.getDescription();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        repositoryData = new RepositoryData(name, activeBranchName, numberOfBranches, lastCommitDate, lastCommitMessage);
//        userData.AddRepositoryDataToRepositorysDataList(repositoryData);
//    }
//
//
//    ///////////////////////
//    // REPOSITORIES PAGE //
//    ///////////////////////
//
//    // TODO debug
//    public void importRepositoryToLoggedInUser(String xmlfileContent, String loggedInUsername) throws Exception {
//        manager.importFromXMLToHub(xmlfileContent, MAGITHUB_FOLDER_PATH + File.separator + loggedInUsername);
//        getUserByName(loggedInUsername).addRepository(manager.getActiveRepository());
//    }
//
//
//
//    ////////////////////////////
//    // ACTIVE REPOSITORY PAGE //
//    ////////////////////////////
//
//    // TODO debug
//    public void loadUsersRepository(String repoName, String loggedInUsername) throws IOException, ParseException {
//        manager.switchRepository(Paths.get(MAGITHUB_FOLDER_PATH + File.separator + loggedInUsername + File.separator + repoName));
//    }
//
//
//}