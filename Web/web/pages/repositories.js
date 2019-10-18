//var USERS_DATA_URL = buildUrlWithContextPath("usersInformation");
var CURRENT_USER_DATA_URL = buildUrlWithContextPath("currentUserInformation");
var OTHER_USERS_DATA_URL = buildUrlWithContextPath("otherUsersInformation");
var NEW_REPOSITORY_URL = buildUrlWithContextPath("newRepository");
var CURRENT_USER_DATA;
var OTHER_USERS_DATA;


$(function () {
    initializeWindow();
});

$(function () {
    // setInterval(refreshOtherUsersDisplay, 2000);
    setInterval(refreshCurrentUserData, 2000);
});


function initializeWindow() {
    ajaxCurrentUserData(function (currentUserData) {
        setCurrentUserDataVar(currentUserData);
        displayCurrentUserRepositories();
        displayTopBarUsername();
    });
    // refreshOtherUsersDisplay();
}



function refreshCurrentUserData() {
    ajaxCurrentUserData(function (currentUserData) {
        setCurrentUserDataVar(currentUserData)
    });
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

function setCurrentUserDataVar(currentUserData) {
    CURRENT_USER_DATA = currentUserData;
}

function setOtherUsersDataVar(otherUserData) {
    OTHER_USERS_DATA = otherUserData;
}

function displayTopBarUsername() {
    $("#topBarUsername").text(CURRENT_USER_DATA.userName);
}

function displayCurrentUserRepositories() {
    $("#currentUserReposTable").empty();
    $.each(CURRENT_USER_DATA.repositoriesDataList || [], addSingleRepositoryDataToCurrentUser);
}

function addSingleRepositoryDataToCurrentUser(index, currentUserSingleRepositoryData) {
    var watchButtonID ="watch"+ currentUserSingleRepositoryData.name;
    var singleRepositoryDataRow = createCurrentUserSingleRepositoryData(currentUserSingleRepositoryData, index);

    $("#currentUserReposTable").append(singleRepositoryDataRow);
}

function createCurrentUserSingleRepositoryData(currentUserSingleRepositoryData, index) {
    return  "<tr>\n" +
        "<td>" + currentUserSingleRepositoryData.name + "</td>\n" +
        "<td>" + currentUserSingleRepositoryData.activeBranchName + "</td>\n" +
        "<td>" + currentUserSingleRepositoryData.numberOfBranches + "</td>\n" +
        "<td>" + currentUserSingleRepositoryData.lastCommitDate + "</td>\n" +
        "<td>" + currentUserSingleRepositoryData.lastCommitMessage + "</td>" +
        "</tr>\n";
}



// function refreshOtherUsersDisplay() {
//     ajaxOtherUsersData(function (otherUsersData) {
//         ajaxOtherUsersDataCallback(otherUsersData)
//     });
// }


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
                success: repositoryAjaxSucceededCallback(messagee, true),
                error: repositoryAjaxSucceededCallback(messagee, false)

            }
        );
    };
    reader.readAsText(file);

    function repositoryAjaxSucceededCallback(message, success) {
        ShowMessage(message, success);

        refreshCurrentUserData();
        $(fileInput).val(null);
    }
}




// <a href="#myModal" data-toggle="modal">Click to Open Confirm Modal</a>



function ShowMessage(message, success) {
    if (success) {
        $('#successModal').modal('show');
    } else {
        $('#failureModal').modal('show');

    }


    var modal = $("#newRepositoryMessageModal")[0];
    var span = document.getElementsByClassName("closeRepositoryMessage")[0];
    var content = document.getElementById("newRepositoryMessageContent");
    content.textContent = message;
    modal.style.display = "block";
}






// function createOtherUserButton(otherUserSingleRepositoryData) {
//     return "<tr>\n" +
//         "<td>\n" +
//         "<div><a class=\"btn btn-light\" data-toggle=\"collapse\" aria-expanded=\"false\" aria-controls=\"collapse-1\" href=\"#collapse-1\" role=\"button\">" + otherUserSingleRepositoryData.name + "</a>\n" +
//         "<div class=\"collapse\" id=\"collapse-1\"><a class=\"btn btn-light btn-icon-split\" role=\"button\" style=\"margin-top: 11px;\"><span class=\"text-black-50 icon\"><i class=\"far fa-copy\"></i></span><span class=\"text-dark text\">Repo 1111</span></a><a class=\"btn btn-light btn-icon-split\" role=\"button\" style=\"margin-top: 11px;\"><span class=\"text-black-50 icon\"><i class=\"far fa-copy\"></i></span><span class=\"text-dark text\">Repo 2222</span></a><a class=\"btn btn-light btn-icon-split\" role=\"button\" style=\"margin-top: 11px;\"><span class=\"text-black-50 icon\"><i class=\"far fa-copy\"></i></span><span class=\"text-dark text\">Repo 2222</span></a><a class=\"btn btn-light btn-icon-split\" role=\"button\" style=\"margin-top: 11px;\"><span class=\"text-black-50 icon\"><i class=\"far fa-copy\"></i></span><span class=\"text-dark text\">Repo 2222</span></a></div>\n" +
//         "</div>\n" +
//         "</td>\n" +
//         "</tr>";
// }
//////////////////////////////////////////////////////////////////////




// function addSingleOtherUserRow(otherUserSingleRepositoryData) {
//     var otherUserRow = createOtherUserButton(otherUserSingleRepositoryData);
//     $("#otherUsersList").append(otherUserRow);
// }
//
// function refreshOtherUsersList() {
//     $("#otherUsersList").empty();
//     $.each(OTHER_USERS_DATA || [], addSingleOtherUserRow);
// }



// function ajaxOtherUsersData(callback) {
//     $.ajax({
//         url: OTHER_USERS_DATA_URL,
//         dataType: "json",
//         success: function (otherUsersData) {
//             callback(otherUsersData);
//         }
//     });
// }

// function ajaxOtherUsersDataCallback(otherUsersData) {
//     OTHER_USERS_DATA = otherUsersData;
//     refreshOtherUsersList();
// }

