//var USERS_DATA_URL = buildUrlWithContextPath("usersInformation");
var CURRENT_USER_DATA_URL = buildUrlWithContextPath("currentUserInformation");
var OTHER_USERS_DATA_URL = buildUrlWithContextPath("otherUsersInformation");
var NEW_REPOSITORY_URL = buildUrlWithContextPath("newRepository");
var CURRENT_USER_DATA;
var OTHER_USERS_DATA;

// TODO edit
function createCurrentUserSingleRepositoryData(currentUserSingleRepositoryData) {
    return  "<tr>\n" +
            "<td>" + currentUserSingleRepositoryData.name + "</td>\n" +
            "<td>" + currentUserSingleRepositoryData.activeBranchName + "</td>\n" +
            "<td>" + currentUserSingleRepositoryData.numberOfBranches + "</td>\n" +
            "<td>" + currentUserSingleRepositoryData.lastCommitDate + "</td>\n" +
            "<td>" + currentUserSingleRepositoryData.lastCommitMessage + "</td>" +
            "</tr>\n";
}

// TODO edit
function addSingleRepositoryDataToCurrentUser(index, currentUserSingleRepositoryData) {
    var watchButtonID ="watch"+ currentUserSingleRepositoryData.name;
    var singleRepositoryDataButton = createCurrentUserSingleRepositoryData(currentUserSingleRepositoryData,watchButtonID);

    $("#accordion-2").append(singleRepositoryDataButton);
    document.getElementById(watchButtonID).onclick = function () {
        watch(currentUserSingleRepositoryData.name);
    }
}

// TODO edit
function showCurrentUserRepositories() {
    $("#accordion-2").empty();

    $.each(CURRENT_USER_DATA.repositoriesDataList || [], addSingleRepositoryDataToCurrentUser);
}


// TODO edit
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
        $(fileInput).val(null);
    }
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

// TODO edit
function createOtherUserSingleRepositoryData(otherUserSingleRepositoryData) {

    return "<div class=\"card otherUsersRepositoryItem\">\n" +
        "<div class=\"card-header\" role=\"tab\">\n" +
        "<h5 class=\"mb-0\"><a data-toggle=\"collapse\" aria-expanded=\"true\" aria-controls=\"accordion-2 .item-1\" href=\"#accordion-2 .item-1\">" + otherUserSingleRepositoryData.name + "</a><button class=\"btn btn-primary btn-sm float-right\" type=\"button\">Fork</button></h5>\n" +
        "</div>\n" +
        "<div class=\"collapse show item-1 repositoryData\" role=\"tabpanel\" data-parent=\"#accordion-2\">\n" +
        "<div class=\"card-body\"><small class=\"d-md-flex justify-content-md-start\">Active branch: &nbsp" + otherUserSingleRepositoryData.activeBranchName + "</small>" +
        "<small class=\"d-md-flex d-lg-flex justify-content-md-start justify-content-lg-start\">Number of branches :" + otherUserSingleRepositoryData.numberOfBranches + "&nbsp;</small>" +
        "<code class=\"text-warning d-md-flex justify-content-md-start\">Last commit's time stamp: " + otherUserSingleRepositoryData.lastCommitDate + "</code>" +
        "<em class=\"d-md-flex d-lg-flex justify-content-md-start justify-content-lg-start\">Last commit's message: " + otherUserSingleRepositoryData.lastCommitMessage + " </em>" + "</div>\n" +
        "</div>\n" +
        "</div>";
}

// TODO edit
function fork() {

}

// TODO edit
// function addSingleRepositoryDataToOtherUser(index, otherUserSingleRepositoryData) {
//     var singleRepositoryDataButton = createOtherUserSingleRepositoryData(otherUserSingleRepositoryData);
//     $("#accordion-2").append(singleRepositoryDataButton);
// }

// TODO edit
// function showOtherUserRepositories(otherUsername) {
//     $("#accordion-2").empty();
//     var otherUserData = findOtherUserDataInList(otherUsername);
//     $.each(otherUserData.repositoriesDataList || [], addSingleRepositoryDataToOtherUser)
// }

function createOtherUserButton(otherUserSingleRepositoryData) {
    return "<tr>\n" +
                "<td>\n" +
                    "<div><a class=\"btn btn-light\" data-toggle=\"collapse\" aria-expanded=\"false\" aria-controls=\"collapse-1\" href=\"#collapse-1\" role=\"button\">" + otherUserSingleRepositoryData.name + "</a>\n" +
                        "<div class=\"collapse\" id=\"collapse-1\"><a class=\"btn btn-light btn-icon-split\" role=\"button\" style=\"margin-top: 11px;\"><span class=\"text-black-50 icon\"><i class=\"far fa-copy\"></i></span><span class=\"text-dark text\">Repo 1111</span></a><a class=\"btn btn-light btn-icon-split\" role=\"button\" style=\"margin-top: 11px;\"><span class=\"text-black-50 icon\"><i class=\"far fa-copy\"></i></span><span class=\"text-dark text\">Repo 2222</span></a><a class=\"btn btn-light btn-icon-split\" role=\"button\" style=\"margin-top: 11px;\"><span class=\"text-black-50 icon\"><i class=\"far fa-copy\"></i></span><span class=\"text-dark text\">Repo 2222</span></a><a class=\"btn btn-light btn-icon-split\" role=\"button\" style=\"margin-top: 11px;\"><span class=\"text-black-50 icon\"><i class=\"far fa-copy\"></i></span><span class=\"text-dark text\">Repo 2222</span></a></div>\n" +
                    "</div>\n" +
                "</td>\n" +
            "</tr>";
}

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
