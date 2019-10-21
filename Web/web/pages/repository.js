var CURRENT_USER_DATA_URL = buildUrlWithContextPath("currentUserInformation");
var CURRENT_REPOSITORY_DATA_URL = buildUrlWithContextPath("currentRepositoryInformation");
var CURRENT_USER_DATA;
var CURRENT_REPOSITORY_DATA;

$(function () {
    initializeWindow();
});

function initializeWindow() {
    refreshCurrentUserData();
    refresRepositoryData();
}

// USER DATA
function refreshCurrentUserData() {
    ajaxCurrentUserData(function (currentUserData) {
        CURRENT_USER_DATA = currentUserData;
        $("#topBarUsername").text( CURRENT_USER_DATA.userName);
        displaySideMenuRepositories(currentUserData);
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

function displaySideMenuRepositories(currentUserData) {
    $.each(currentUserData.repositoriesDataList || [] , addSingleRepoSideMenuLink)
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

//  REPOSITORY DATA
function refresRepositoryData() {
    ajaxRepositoryData(function (data) {
        CURRENT_REPOSITORY_DATA = data;
        $("#shown-repo-headline").text(data.name);

    });
}

function ajaxRepositoryData(callback) {
    $.ajax({

    url: CURRENT_REPOSITORY_DATA_URL,
        dataType: "json",
        success: function (currentUserData) {
            callback(currentUserData);
        }
    });
}



function replaceSpacesWithUndersore(str){
    return str !== undefined ? str.replace(/ /g,"_") : str;
}