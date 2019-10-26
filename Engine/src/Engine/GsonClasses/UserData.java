package Engine.GsonClasses;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class UserData {

    private String userName;
    private List<RepositoryData> repositoriesDataList = new ArrayList<>();
    private List<PullRequestData> pullRequestsDataList = new LinkedList<>();


    public UserData(String userName){
        this.userName = userName;
    }

    public UserData(String userName, List<PullRequestData> pullRequestsDataList){
        this.userName = userName;
        this.pullRequestsDataList = pullRequestsDataList;
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

    public void AddRepositoryDataToRepositorysDataList(RepositoryData repositoryData) {
        this.repositoriesDataList.add(repositoryData);
    }
    public void AddPullRequestDataToPullRequestsDataList(PullRequestData pullRequestData) {
        this.pullRequestsDataList.add(pullRequestData);
    }
}
