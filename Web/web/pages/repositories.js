var CURRENT_USER_DATA_URL = buildUrlWithContextPath("currentUserInformation");
var OTHER_USERS_DATA_URL = buildUrlWithContextPath("otherUsersInformation");
var NEW_REPOSITORY_URL = buildUrlWithContextPath("newRepository");
var FORK_REPOSITORY_URL = buildUrlWithContextPath("forkRepository");
var REPOSITORY_INFO_URL = buildUrlWithContextPath("repositoryInformation");
var LOAD_REPOSITORY_URL = buildUrlWithContextPath("loadRepository");
var CURRENT_USER_DATA;
var OTHER_USERS_DATA;


$(function () {
    initializeWindow();
});

function initializeWindow() {
    refreshCurrentUserData();
    refreshOtherUsersData();
    console.log()
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
    $(singleRepositoryDataRow).on('click-row.bs.table', function (e, row, $element) {
        loadRepository(currentUserSingleRepositoryData.name);
    });
    $("#currentUserReposTable").append(singleRepositoryDataRow);

}
function createCurrentUserSingleRepositoryDataHTML(currentUserSingleRepositoryData) {
    return  $("<tr>\n" +
                "<td>" + currentUserSingleRepositoryData.name + "</td>\n" +
                "<td>" + currentUserSingleRepositoryData.activeBranchName + "</td>\n" +
                "<td>" + currentUserSingleRepositoryData.numberOfBranches + "</td>\n" +
                "<td>" + currentUserSingleRepositoryData.lastCommitDate + "</td>\n" +
                "<td>" + currentUserSingleRepositoryData.lastCommitMessage + "</td>" +
            "</tr>\n");
}

// TODO user repository make it clickable with link




function loadRepository(repositoryName) {
    ajaxLoadRepository(repositoryName);
}

function ajaxLoadRepository(repositoryName) {
    $.ajax({
        url: LOAD_REPOSITORY_URL,
        data:{
            repositoryName: repositoryName
        },
        success: (message) => {
            if(message.success === false) {
                ShowModal(message)
            }
        }
    });
}




//  SIDE MENU
function displaySideMenuRepoLinks() {
    $.each(CURRENT_USER_DATA.repositoriesDataList || [], addSingleRepoSideMenuLink);
}

function addSingleRepoSideMenuLink(index, currentUserSingleRepositoryData) {
    if (!$("#side-menu-repo-links").find('#' + replaceSpacesWithUndersore(currentUserSingleRepositoryData.name) + '-side-link').length) {
        var singleRepositoryData = createSideMenuSingleRepositoryLink(currentUserSingleRepositoryData);
        singleRepositoryData.on("click", function() {
            loadRepository(currentUserSingleRepositoryData.name);
        });
        $("#side-menu-repo-links").append(singleRepositoryData);
    }
}

function createSideMenuSingleRepositoryLink(currentUserSingleRepositoryData){
    return  $('<li class="nav-item" role="presentation" id="' + replaceSpacesWithUndersore(currentUserSingleRepositoryData.name) + '-side-link">  '  +
            '    <a class="nav-link" href="repository.html" style="padding-top: 5px;padding-bottom: 5px;padding-left: 30px;">  '  +
            '        <i class="fas fa-tachometer-alt"></i>  '  +
            '        <span>' +
                         currentUserSingleRepositoryData.name +
            '        </span>  '  +
            '    </a>  '  +
            '</li>  ' );
}
// TODO side menu user repository make it clickable with link











//  LOWER TABLE (other users date)
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
    updateOtherUserRepositoryButtons(otherUserData);
}
function createOtherUserSingleButtonHTML(otherUserData) {
    var collapseContentID = replaceSpacesWithUndersore(otherUserData.userName) + '-collapse';
    return  '<tr ' + 'id="' + replaceSpacesWithUndersore(otherUserData.userName) + '-row"> '  +
            ' <td>  '  +
            ' <div>  '  +
            '     <a class="btn btn-light" data-toggle="collapse" aria-expanded="false" href="#' + collapseContentID + '" role="button">  '  +
                      otherUserData.userName  +
            '     </a>  '  +
            '     <div class="collapse" ' + 'id="' + replaceSpacesWithUndersore(otherUserData.userName) + '-collapse">'  +
            '     </div>  '  +
            ' </div>  '  +
            ' </td>  '  +
            '</tr>  ' ;
}
function updateOtherUserRepositoryButtons(otherUserData) {
    $('#' + replaceSpacesWithUndersore(otherUserData.userName) + '-collapse').empty();
    otherUserData.repositoriesDataList.forEach(function(element) {
        var forkButton = createSingleOtherUserRepositoryForkButton(element.name, otherUserData.userName);
        var collapseContentID = replaceSpacesWithUndersore(otherUserData.userName) + '-collapse';
        forkButton.on("click", function() {
            forkRepository(otherUserData.userName, element.name);
        })
        $('#' + collapseContentID).append(forkButton);
    });
}

function createSingleOtherUserRepositoryForkButton(repositoryName, username) {  // returns an elemene beacuse of the $ - so can be appended easily later
    return  $('<a class="btn btn-light btn-icon-split" role="button" style="margin-top: 11px;" id="' + replaceSpacesWithUndersore(username) + '-' + replaceSpacesWithUndersore(repositoryName) + '-forkButton">  '  +
            '    <span class="text-black-50 icon">  '  +
            '        <i class="far fa-copy"></i>  '  +
            '    </span>  '  +
            '    <span class="text-dark text">  '  +
                    repositoryName  +
            '    </span>  '  +
            '</a>  ');
}
function fork(otherUserData, remoteRepository) {

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
                    repositoryAjaxSucceededCallback(message)}
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







function forkRepository(otherUsername, repositoryName) {
    ajaxForkRepository(otherUsername, repositoryName);
}

function ajaxForkRepository(otherUsername, otherUserRepositoryName) {
    $.ajax({
        url: FORK_REPOSITORY_URL,
        data:{
            otherUsername : otherUsername,
            otherUserRepositoryName: otherUserRepositoryName
        },
        success: (message) => {
            ShowModal(message)
        }
    });
}





//
// function watchRepositoryInfo(repositoryName) {
//     ajaxWatchRepositoryInfo(repositoryName);
// }
// function ajaxWatchRepositoryInfo(repositoryName) {
//     $.ajax({
//         url: REPOSITORY_INFO_URL,
//         dataType:"json",
//         data:{
//             repositoryNameToWatch : repositoryName
//         },
//         success:function(newUrl){
//             var fullUrl = buildUrlWithContextPath(newUrl);
//             window.location.replace(fullUrl);
//         }
//     });
//
// }


function replaceSpacesWithUndersore(str){
    return str !== undefined ? str.replace(/ /g,"_") : str;
}

$(function () {
    setInterval(refreshOtherUsersData, 2000);
    setInterval(refreshCurrentUserData, 2000);
});
