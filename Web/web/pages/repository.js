var CURRENT_USER_DATA_URL = buildUrlWithContextPath("currentUserInformation");
var CURRENT_REPOSITORY_DATA_URL = buildUrlWithContextPath("currentRepositoryInformation");
var CHECKOUT_URL = buildUrlWithContextPath("checkout");
var WORKING_COPY_URL = buildUrlWithContextPath("workingCopy");

var CURRENT_USER_DATA;
var CURRENT_REPOSITORY_DATA;
var WORKING_COPY_LIST;
var IS_RTB;
var IS_HEAD_RTB;

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

function createSideMenuSingleRepositoryLink(currentUserSingleRepositoryData) {
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
        displayBranchesCheckoutButtons();
        refreshForkedRepositoriesTable();
        refreshCommitsTable();
        refreshWorkingCopyList();
        refreshRemoteButtons();
    });
}

function ajaxRepositoryData(callback) {
    $.ajax({
        url: CURRENT_REPOSITORY_DATA_URL,
        dataType: "json",
        success: function (currentRepositoryData) {
            callback(currentRepositoryData);
        }
    });
}

function refreshRemoteButtons() {
    $("#new-branch-button").removeAttr('disabled').button("refresh");
    IS_RTB = CURRENT_REPOSITORY_DATA.isRTB;

    if(IS_RTB) {
        $("#pull-button").removeAttr('disabled').button("refresh");
        $("#pull-request-button").removeAttr('disabled').button("refresh");
        IS_HEAD_RTB === true ?
            $("#push-button").attr("disabled", "disabled").button("refresh") :
                $("#push-button").removeAttr('disabled').button("refresh");

    } else {
        $("#push-button").attr("disabled", "disabled").button("refresh");
        $("#pull-request-button").attr("disabled", "disabled").button("refresh");
        $("#pull-button").attr("disabled", "disabled").button("refresh");
    }
}

function refreshCommitsTable() {
    $("#commitsDataTable").empty();
    $.each(CURRENT_REPOSITORY_DATA.commitsList || [], addSingleCommitRow);
}

function displayBranchesCheckoutButtons() {
    $("#branchCheckoutButtons").empty();
    $.each(CURRENT_REPOSITORY_DATA.branchesDataList || [], addSingleBranchCheckoutButton);
}

function refreshForkedRepositoriesTable() {
    $("#forkedRepositoriesTable").empty();
    $.each(CURRENT_REPOSITORY_DATA.forkedMap || [], addSingleForkedRepositoryRow)
}

function refreshWorkingCopyList() {
    ajaxWorkingCopy("refreshWC", function(workingCopyData) {
        WORKING_COPY_LIST = workingCopyData;
        $("#workingCopyList").empty();
        $.each(WORKING_COPY_LIST.components || [], addSingleWorkingCopyComponent);
    });
}

function ajaxWorkingCopy(action, callback) {
    $.ajax({
        url: WORKING_COPY_URL,
        dataType: "json",
        data: {
            wcAction: action
        },
        success: function (response) {
            callback(response);
        }
    });
}

function addSingleCommitRow(index, commitData) {
    let singleCommitRow = createSingleCommitRow(commitData);
    $("#commitsDataTable").append(singleCommitRow);
}

function addSingleBranchCheckoutButton(index, branchData) {
    let branchCheckoutButtonHTML = createBranchCheckoutButton(branchData);
    $(branchCheckoutButtonHTML).on('click', function () {
        console.log("checkout to " + branchData.name);
    })
    $("#branchCheckoutButtons").append(branchCheckoutButtonHTML);
}

function addSingleForkedRepositoryRow(key, value) {
    let forkedRepoRow = createForkedRepositoryRow(key, value);
    $("#forkedRepositoriesTable").append(forkedRepoRow);
}

function addSingleWorkingCopyComponent(index, componentData) {
    let singleWorkingCopyRow = createSingleWorkingCopyRow(componentData);
    $("#workingCopyList").append(singleWorkingCopyRow);
}

function createSingleCommitRow(commitData) {
    let tableRow =  $('   <tr>  '  +
        ' <td>' + commitData.SHA1 + ' </td>  '  +
        ' <td> ' + commitData.message + ' </td>  '  +
        ' <td> ' + commitData.dateCreated + ' </td>  '  +
        ' <td> ' + commitData.author + ' </td>  '  +
        ' <td> ' + createPointingBranchesTags(commitData) + '</td>' +
        '</tr> ' );
    return tableRow;
}

function createPointingBranchesTags(commitData) {
    var buttonsStr = "";
    $.each(commitData.pointingBranches || [] , (index, branch) => {
        buttonsStr += '<button class="btn btn-dark" type="button" style="margin: 3px;font-size: 10px;padding-top: 3px;padding-right: 6px;padding-bottom: 3px;padding-left: 6px;" disabled="">'
                + branch
                + '</button>';
    });
    return buttonsStr;
}

function createBranchCheckoutButton(branchData) {
    let btnClass;
    var disabled = false;

    if (CURRENT_REPOSITORY_DATA.activeBranchName === branchData.name) {
        IS_HEAD_RTB = branchData.isRtb;
        disabled = true;
        if (branchData.isRtb) {
            btnClass = "btn btn-outline-warning";
        } else {
            btnClass = "btn btn-warning";
        }
    } else if (branchData.name === "master") {
        if (branchData.isRtb) {
            btnClass = "btn btn-outline-success"
        } else {
            btnClass = "btn btn-success";
        }
    } else {
        if (branchData.isRtb) {
            btnClass = "btn btn-outline-secondary";
        } else {
            btnClass = "btn btn-secondary";
        }
    }

    let btn = $("<button type='button' style='margin: 5px;'>" + branchData.name + "</button>\n");
    if (disabled) {
        btn.attr("disabled", "disabled").button("refresh");
    }
    btn.on("click", function () {
        checkout(branchData.name);
        IS_HEAD_RTB = branchData.isRtb;
    });
    return btn.addClass(btnClass);
}

function createSingleWorkingCopyRow(componentData) {
    var icon = (componentData.type === "folder") ? "<i class=\"fas fa-folder-open\"></i>" : "<i class=\"far fa-file-alt\"></i>";
    var spaces = "";
    // TODO add indentation

    let btn = $(
        '<tr>'  +
        '   <td>  '  +
        '       <a href="#">' +
        spaces + icon + '  ' + componentData.name +
        '       </a></td>  '  +
        '   <td class="text-center">  '  +
        '       <button class="btn btn-danger btn-circle ml-1" type="button">  '  +
        '           <i class="fas fa-trash text-white"></i>  '  +
        '       </button>  '  +
        '       <button class="btn btn-warning btn-circle ml-1" type="button">  '  +
        '           <i class="fas fa-edit text-white"></i>  '  +
        '       </button>  '  +
        '   </td>  '  +
        '</tr>'
    );
    return btn;
}

function createForkedRepositoryRow(key, value) {
    return  '<tr>' +
            '<td>' + value + '</td>'  +
            '<td>' + key + '</td>'  +
            '</tr>';
}

function replaceSpacesWithUndersore(str){
    return str !== undefined ? str.replace(/ /g,"_") : str;
}


// manager functions
function checkout(branchName) {
    var reader = new FileReader();

        $.ajax(
            {
                url: CHECKOUT_URL,
                dataType: "json",
                data: {
                    branchToCheckout: branchName
                },
                success: (message) => {
                    checkoutCallback(message)
                }
            }
        );

    function checkoutCallback(message) {
        // var jsonResponse = JSON.parse(message);
        if (message.success) {
            refresRepositoryData();
        } else {
            ShowModal(message);
        }
    }
}