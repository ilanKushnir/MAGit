package Engine.GsonClasses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class UserData {

    private String userName;
    private List<RepositoryData> repositoriesDataList = new ArrayList<>();
    private List<PullRequestData> pullRequestsDataList = new LinkedList<>();
    private HashMap<String, LinkedList<String>> forkedRepositories = new HashMap<>();   //  <UserName, ForekedRepoName>



    public UserData(String userName){
        this.userName = userName;
    }

    public UserData(String userName, List<PullRequestData> pullRequestsDataList, HashMap<String, LinkedList<String>> forkedRepositories){
        this.userName = userName;
        this.pullRequestsDataList = pullRequestsDataList;
        this.forkedRepositories = forkedRepositories;
    }

    public String getUserName() {
        return userName;
    }
    public List<RepositoryData> getRepositoriesDataList() {
        return this.repositoriesDataList;
    }

    public List<PullRequestData> getPullRequestsDataList() {
        return pullRequestsDataList;
    }

    public HashMap<String, LinkedList<String>> getForkedRepositories() { return forkedRepositories;
    }

    public void AddRepositoryDataToRepositorysDataList(RepositoryData repositoryData) { ////////////////////
        this.repositoriesDataList.add(repositoryData);
    }
    public void AddPullRequestDataToPullRequestsDataList(PullRequestData pullRequestData) {
        this.pullRequestsDataList.add(pullRequestData);
    }
//    public void AddForkedReposirotyToForkedRepositoriesDataList(String userName, String forkedRepository) {
//        this.forkedRepositories.put(userName, forkedRepository);          need to be changed if used!!!
//    }
}
