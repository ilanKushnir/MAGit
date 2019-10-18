package Engine.GsonClasses;

import java.util.ArrayList;
import java.util.List;

public class UserData {

    final private String userName;
    final private List<RepositoryData> repositoriesDataList = new ArrayList<>();

    public UserData(String userName){
        this.userName = userName;
    }
    public String getUserName() {
        return userName;
    }
    public List<RepositoryData> getRepositoriesDataList() {
        return this.repositoriesDataList;
    }
    public void AddRepositoryDataToRepositorysDataList(RepositoryData repositoryData) {
        this.repositoriesDataList.add(repositoryData);
    }
}
