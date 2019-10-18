package Engine;

import Engine.GsonClasses.RepositoryData;
import Engine.GsonClasses.UserData;

import java.io.File;
import java.io.IOException;
import java.util.*;

/*
Adding and retrieving users is synchronized and in that manner - these actions are thread safe
Note that asking if a user exists (isUserExists) does not participate in the synchronization and it is the responsibility
of the user of this class to handle the synchronization of isUserExists with other methods here on it's own
 */
public class MAGitHubManager {

    private final String MAGITHUB_FOLDER_PATH = "c:" + File.separator + "magit-ex3";
    private Manager manager;
    private final Set<String> usersSet;

    public MAGitHubManager() {
        usersSet = new HashSet<>();
        manager = new Manager();
    }

    public synchronized void addUser(String username) {
        usersSet.add(username);
    }

    public synchronized void removeUser(String username) {
        usersSet.remove(username);
    }

    public synchronized Set<String> getUsers() {
        return Collections.unmodifiableSet(usersSet);
    }

    public boolean isUserExists(String username) {
        return usersSet.contains(username);
    }



    public List<UserData> GetOtherUsersData(String currentUserName) {
        List<UserData> otherUsersData = new ArrayList<>();

        UserData userDataToAdd;
        File usersDirectory = new File(MAGITHUB_FOLDER_PATH);

        for (File file : usersDirectory.listFiles()) {
            userDataToAdd = getUserData(file.getName());
            if (!file.getName().equals(currentUserName)) {
                otherUsersData.add(userDataToAdd);
            }
        }
        return otherUsersData;
    }

    private UserData getUserData(String userName) {
        UserData userData = new UserData(userName);
        File userDirectory = new File(MAGITHUB_FOLDER_PATH + File.separator + userName);
        for (File file : userDirectory.listFiles()) {
            addRepositoryDirectoryToUserData(userData, file, userName);
        }
        return userData;
    }

    private void addRepositoryDirectoryToUserData(UserData userData, File directoryFile, String userName) {
        RepositoryData repositoryData;
        String name;
        Integer numberOfBranches;
        String activeBranchName = null;
        String lastCommitDate = null;
        String lastCommitMessage = null;
        String lastCommitSha1;
        String repositoryPath = MAGITHUB_FOLDER_PATH + File.separator + userName + File.separator + directoryFile.getName();
        String repositoryBranchesPath = repositoryPath + File.separator + ".magit" + File.separator + "branches";

        name = directoryFile.getName();
        numberOfBranches = new File(repositoryBranchesPath).listFiles().length;

        try {
            activeBranchName = manager.readFileToString(repositoryBranchesPath + File.separator + "HEAD.txt");
            lastCommitSha1 = manager.readFileToString(repositoryBranchesPath + File.separator + activeBranchName + ".txt");

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