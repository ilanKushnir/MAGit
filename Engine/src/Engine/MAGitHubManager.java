package Engine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class MAGitHubManager {
    private HashSet<User> users;
    private Manager manager;
    private final String MAGITHUB_FOLDER_PATH = "c:" + File.separator + "magit-ex3";

    // TODO debug
    public MAGitHubManager(Manager manager) {
        this.users = new HashSet<>();
        this.manager = manager;

        File file = new File(MAGITHUB_FOLDER_PATH);
        file.mkdir();
    }


    public synchronized void removeUser(User user) { users.remove(user); }
    public synchronized Set<User> getUsers() {
        return Collections.unmodifiableSet(users);
    }
    public boolean isUserExists(String userName) { return users.stream().anyMatch(userName::equals); }


    ////////////////
    // LOGIN PAGE //
    ////////////////

    public synchronized void addUser(User newUser) { users.add(newUser); }

    // TODO debug
    public User getUserByName(String username) {
        User out = null;

        for(User userObj : users) {
            if (userObj.getUserName().equals(username)) {
                out = userObj;
            }
        }

        return out;
    }


    /////////////////////
    // BOTH REPO PAGES //
    /////////////////////

    // TODO debug
    public void cloneOtherUsersRepository(String loggedInUsername, String otherUsername, String repoName) throws Exception {

        manager.clone(Paths.get(MAGITHUB_FOLDER_PATH + File.separator + otherUsername + File.separator + repoName),
                Paths.get(MAGITHUB_FOLDER_PATH + File.separator + loggedInUsername + File.separator + repoName));
    }


    ///////////////////////
    // REPOSITORIES PAGE //
    ///////////////////////

    // TODO debug
    public void importRepositoryToLoggedInUser(String xmlfileContent, String loggedInUsername) throws Exception {
        manager.importFromXMLToHub(xmlfileContent, MAGITHUB_FOLDER_PATH + File.separator + loggedInUsername);
        getUserByName(loggedInUsername).addRepository(manager.getActiveRepository());
    }



    ////////////////////////////
    // ACTIVE REPOSITORY PAGE //
    ////////////////////////////

    // TODO debug
    public void loadUsersRepository(String repoName, String loggedInUsername) throws IOException, ParseException {
        manager.switchRepository(Paths.get(MAGITHUB_FOLDER_PATH + File.separator + loggedInUsername + File.separator + repoName));
    }


}