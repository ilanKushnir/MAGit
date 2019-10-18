//var USERS_DATA_URL = buildUrlWithContextPath("usersInformation");
var CURRENT_USER_DATA_URL = buildUrlWithContextPath("currentUserInformation");
var OTHER_USERS_DATA_URL = buildUrlWithContextPath("otherUsersInformation");
var NEW_REPOSITORY_URL = buildUrlWithContextPath("newRepository");
var CURRENT_USER_DATA;
var OTHER_USERS_DATA;




function initializeWindow() {
    ajaxCurrentUserData(function (currentUserData) {
        ajaxCurrentUserDataCallback(currentUserData);
        showCurrentUserRepositories();
    });
    refreshOtherUsersDisplay();
}

$(function () {
    initializeWindow();
});

function ajaxCurrentUserDataCallback(currentUserData) {
    CURRENT_USER_DATA = currentUserData;
}

function refreshCurrentUserData() {
    ajaxCurrentUserData(function (currentUserData) {
        ajaxCurrentUserDataCallback(currentUserData)
    });
}

function refreshOtherUsersDisplay() {
    ajaxOtherUsersData(function (otherUsersData) {
        ajaxOtherUsersDataCallback(otherUsersData)
    });
}

$(function () {
    setInterval(refreshOtherUsersDisplay, 2000);
    setInterval(refreshCurrentUserData, 2000);
});






function addNewRepositoryToCurrentUser(event) {
    var file = event.target.files[0];
    ajaxNewRepository(file, ShowMessage);
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
                success: repositoryAjaxSucceededCallback

            }
        );
    };
    reader.readAsText(file);

    function repositoryAjaxSucceededCallback(message) {
        ShowMessage(message);
        refreshCurrentUserData();
        $(fileInput).val(null);
    }
}



function addSingleRepositoryDataToCurrentUser(index, currentUserSingleRepositoryData) {
    var watchButtonID ="watch"+ currentUserSingleRepositoryData.name;
    var singleRepositoryDataButton = createCurrentUserSingleRepositoryData(currentUserSingleRepositoryData,watchButtonID);

    $("#currentUserReposTable").append(singleRepositoryDataButton);
    // document.getElementById(watchButtonID).onclick = function () {
    //     watch(currentUserSingleRepositoryData.name);
    // }
}

function showCurrentUserRepositories() {
    $("#accordion-2").empty();
    $.each(CURRENT_USER_DATA.repositoriesDataList || [], addSingleRepositoryDataToCurrentUser);
}


function ShowMessage(message) {
    var modal = $("#newRepositoryMessageModal")[0];
    var span = document.getElementsByClassName("closeRepositoryMessage")[0];
    var content = document.getElementById("newRepositoryMessageContent");
    content.textContent = message;
    modal.style.display = "block";
    span.onclick = function () {
        modal.style.display = "none";
    };
}



function createCurrentUserSingleRepositoryData(currentUserSingleRepositoryData) {
    return  "<tr>\n" +
        "<td>" + currentUserSingleRepositoryData.name + "</td>\n" +
        "<td>" + currentUserSingleRepositoryData.activeBranchName + "</td>\n" +
        "<td>" + currentUserSingleRepositoryData.numberOfBranches + "</td>\n" +
        "<td>" + currentUserSingleRepositoryData.lastCommitDate + "</td>\n" +
        "<td>" + currentUserSingleRepositoryData.lastCommitMessage + "</td>" +
        "</tr>\n";
}


function createOtherUserButton(otherUserSingleRepositoryData) {
    return "<tr>\n" +
        "<td>\n" +
        "<div><a class=\"btn btn-light\" data-toggle=\"collapse\" aria-expanded=\"false\" aria-controls=\"collapse-1\" href=\"#collapse-1\" role=\"button\">" + otherUserSingleRepositoryData.name + "</a>\n" +
        "<div class=\"collapse\" id=\"collapse-1\"><a class=\"btn btn-light btn-icon-split\" role=\"button\" style=\"margin-top: 11px;\"><span class=\"text-black-50 icon\"><i class=\"far fa-copy\"></i></span><span class=\"text-dark text\">Repo 1111</span></a><a class=\"btn btn-light btn-icon-split\" role=\"button\" style=\"margin-top: 11px;\"><span class=\"text-black-50 icon\"><i class=\"far fa-copy\"></i></span><span class=\"text-dark text\">Repo 2222</span></a><a class=\"btn btn-light btn-icon-split\" role=\"button\" style=\"margin-top: 11px;\"><span class=\"text-black-50 icon\"><i class=\"far fa-copy\"></i></span><span class=\"text-dark text\">Repo 2222</span></a><a class=\"btn btn-light btn-icon-split\" role=\"button\" style=\"margin-top: 11px;\"><span class=\"text-black-50 icon\"><i class=\"far fa-copy\"></i></span><span class=\"text-dark text\">Repo 2222</span></a></div>\n" +
        "</div>\n" +
        "</td>\n" +
        "</tr>";
}
//////////////////////////////////////////////////////////////////////




function addSingleOtherUserRow(otherUserSingleRepositoryData) {
    var otherUserRow = createOtherUserButton(otherUserSingleRepositoryData);
    $("#otherUsersList").append(otherUserRow);
}

function refreshOtherUsersList() {
    $("#otherUsersList").empty();
    $.each(OTHER_USERS_DATA || [], addSingleOtherUserRow);
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

function ajaxOtherUsersData(callback) {
    $.ajax({
        url: OTHER_USERS_DATA_URL,
        dataType: "json",
        success: function (otherUsersData) {
            callback(otherUsersData);
        }
    });
}

function ajaxOtherUsersDataCallback(otherUsersData) {
    OTHER_USERS_DATA = otherUsersData;
    refreshOtherUsersList();
}

