package Engine;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class MAGitHubManager {
    private HashSet<User> users;

    public MAGitHubManager() { this.users = new HashSet<>();}

    public synchronized void addUser(User newUser) { users.add(newUser); }

    public synchronized void removeUser(User newUser) { users.remove(newUser); }

    public synchronized Set<User> getUsers() {
        return Collections.unmodifiableSet(users);
    }

    public boolean isUserExists(String userName) { return users.stream().anyMatch(userName::equals); }
}
