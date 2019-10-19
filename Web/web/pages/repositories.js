var CURRENT_USER_DATA_URL = buildUrlWithContextPath("currentUserInformation");
var OTHER_USERS_DATA_URL = buildUrlWithContextPath("otherUsersInformation");
var NEW_REPOSITORY_URL = buildUrlWithContextPath("newRepository");
var CURRENT_USER_DATA;
var OTHER_USERS_DATA;


$(function () {
    initializeWindow();
});

function initializeWindow() {
    refreshCurrentUserData();
    refreshOtherUsersData();
}




function updateUsernamePlaceholders() {
    var currentUserName = CURRENT_USER_DATA.userName;
    $("#topBarUsername").text(currentUserName);
    $("#currentUserReposTableTitle").text(currentUserName + "'s repositories in M.A.Git");
}



function refreshCurrentUserData() {
    ajaxCurrentUserData(function (currentUserData) {
        setCurrentUserDataVar(currentUserData);
        displayCurrentUserRepositories();
        displaySideMenuRepoLinks();
        updateUsernamePlaceholders();
    });
}
function setCurrentUserDataVar(currentUserData) {
    CURRENT_USER_DATA = currentUserData;
}
function ajaxCurrentUserData(callback) {
    $.ajax({
        url: CURRENT_USER_DATA_URL,
        dataType: "json",
        success: function (currentUserData) {
            callback(currentUserData);
        }
    });
}
function displayCurrentUserRepositories() {
    $("#currentUserReposTable").empty();
    $.each(CURRENT_USER_DATA.repositoriesDataList || [], addSingleRepositoryDataToCurrentUser);
}
function addSingleRepositoryDataToCurrentUser(index, currentUserSingleRepositoryData) {
    var singleRepositoryDataRow = createCurrentUserSingleRepositoryDataHTML(currentUserSingleRepositoryData);
    $("#currentUserReposTable").append(singleRepositoryDataRow);
}
function createCurrentUserSingleRepositoryDataHTML(currentUserSingleRepositoryData) {
    return  "<tr>\n" +
                "<td>" + currentUserSingleRepositoryData.name + "</td>\n" +
                "<td>" + currentUserSingleRepositoryData.activeBranchName + "</td>\n" +
                "<td>" + currentUserSingleRepositoryData.numberOfBranches + "</td>\n" +
                "<td>" + currentUserSingleRepositoryData.lastCommitDate + "</td>\n" +
                "<td>" + currentUserSingleRepositoryData.lastCommitMessage + "</td>" +
            "</tr>\n";
}










function displaySideMenuRepoLinks() {
    $.each(CURRENT_USER_DATA.repositoriesDataList || [], addSingleRepoSideMenuLink);
}
function addSingleRepoSideMenuLink(index, currentUserSingleRepositoryData) {
    if (!$("#side-menu-repo-links").find('#' + replaceSpacesWithUndersore(currentUserSingleRepositoryData.name) + '-side-link').length) {
        var singleRepositoryData = createSideMenuSingleRepositoryLink(currentUserSingleRepositoryData);
        $("#side-menu-repo-links").append(singleRepositoryData);
    }
}
function createSideMenuSingleRepositoryLink(currentUserSingleRepositoryData){
    return  '   <li class="nav-item" role="presentation" id="' + replaceSpacesWithUndersore(currentUserSingleRepositoryData.name) + '-side-link">  '  +
            '       <a class="nav-link" href="repository.html" style="padding-top: 5px;padding-bottom: 5px;padding-left: 30px;">  '  +
            '           <i class="fas fa-tachometer-alt"></i>  '  +
            '           <span>' +
                            currentUserSingleRepositoryData.name +
            '           </span>  '  +
            '       </a>  '  +
            '   </li>  ' ;
}











function refreshOtherUsersData() {
    ajaxOtherUsersData(function (currentUserData) {
        setOtherUsersDataVar(currentUserData);
        displayOtherUsersButtons()
    });
}
function ajaxOtherUsersData(callback) {
    $.ajax({
        url: OTHER_USERS_DATA_URL,
        dataType: "json",
        success: function (currentUserData) {
            callback(currentUserData);
        }
    });
}
function setOtherUsersDataVar(otherUsersData) {
    OTHER_USERS_DATA = otherUsersData;
}
function displayOtherUsersButtons() {
    console.log(OTHER_USERS_DATA);
    $.each(OTHER_USERS_DATA || [], addOtherUserButton);
}
function addOtherUserButton(index, otherUserData) {
    if (!$("#otherUsersList").find('#' + replaceSpacesWithUndersore(otherUserData.userName) + '-row').length) {
        var otherUserButton = createOtherUserSingleButtonHTML(otherUserData);
        $("#otherUsersList").append(otherUserButton);
    }
}
function createOtherUserSingleButtonHTML(otherUserData) {
    return  '   <tr ' + 'id="' + replaceSpacesWithUndersore(otherUserData.userName) + '-row">  '  +
            '           <td>  '  +
            '           <div>  '  +
            '               <a class="btn btn-light" data-toggle="collapse" aria-expanded="false" href="#' + replaceSpacesWithUndersore(otherUserData.userName) + '-collapse" role="button">  '  +
                                otherUserData.userName  +
            '               </a>  '  +
            '               <div class="collapse" ' + 'id="' + replaceSpacesWithUndersore(otherUserData.userName) + '-collapse">'  +
            '                   <a class="btn btn-light btn-icon-split" role="button" style="margin-top: 11px;">  '  +
            '                       <span class="text-black-50 icon">  '  +
            '                           <i class="far fa-copy"></i>  '  +
            '                       </span>  '  +
            '                       <span class="text-dark text">  '  +
            '                           Repo 1111  '  +
            '                       </span>  '  +
            '                   </a>  '  +
            '               </div>  '  +
            '           </div>  '  +
            '           </td>  '  +
            '          </tr>  ' ;
}




function addNewRepositoryToCurrentUser(event) {
    var file = event.target.files[0];
    ajaxNewRepository(file, ShowModal);
}

function ajaxNewRepository(file, callback) {
    var reader = new FileReader();
    reader.onload = function () {
        var content = reader.result;
        $.ajax(
            {
                url: NEW_REPOSITORY_URL,
                data: {
                    file: content
                },
                type: 'POST',
                success: (message) => {
                    var response = JSON.parse(message);
                    repositoryAjaxSucceededCallback(response)}
            }
        );
    };
    reader.readAsText(file);

    function repositoryAjaxSucceededCallback(response) {
        ShowModal(response);

        refreshCurrentUserData();
        $('#fileChooser').val(null);
    }
}





function ShowModal(response) {
    if (response.success) {
        document.getElementById("modal-success-content").textContent = response.message;
        $('#successModal').modal('show');
    } else {
        document.getElementById("modal-failure-content").textContent = response.message;
        $('#failureModal').modal('show');
    }
}

function replaceSpacesWithUndersore(str){
    return str.replace(/ /g,"_");;
}

$(function () {
    setInterval(refreshOtherUsersData, 2000);
    setInterval(refreshCurrentUserData, 2000);
});
