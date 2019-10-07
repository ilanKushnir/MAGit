package Engine;

import Engine.Repository;

import java.util.HashSet;
import java.util.Set;

public class User {
    private String userName = "UserName";
    private HashSet<Repository> repositories;

    public User() { repositories = new HashSet<>(); }

    public User(String userName) {
        this();
        this.userName = userName;
    }

    public User(String userName, HashSet<Repository> repositories) {
        this(userName);
        this.repositories = repositories;
    }

    //getters
    public Set<Repository> getRepositories() { return this.repositories; }

    public String getUserName() { return this.userName; }

    //setters
    public void setRepositories(HashSet<Repository> repositories) { this.repositories = repositories; }

    public void setUserName(String userName) { this.userName = userName; }

    public void addRepository(Repository newRepository) { repositories.add(newRepository); }
}
