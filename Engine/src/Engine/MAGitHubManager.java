package Engine;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class MAGitHubManager {
    private HashSet<User> users;
    private Manager manager;
    private final String MAGITHUB_FOLDER_PATH = "c:\\magit-ex3";

    public MAGitHubManager(Manager manager) {
        this.users = new HashSet<>();
        this.manager = manager;

        File file = new File(MAGITHUB_FOLDER_PATH);
        file.mkdir();
    }

    public synchronized void addUser(User newUser) { users.add(newUser); }

    public synchronized void removeUser(User user) { users.remove(user); }

    public synchronized Set<User> getUsers() {
        return Collections.unmodifiableSet(users);
    }

    public boolean isUserExists(String userName) { return users.stream().anyMatch(userName::equals); }



}