var CURRENT_USER_DATA_URL = buildUrlWithContextPath("currentUserInformation");
var CURRENT_REPOSITORY_DATA_URL = buildUrlWithContextPath("currentRepositoryInformation");
var CHECKOUT_URL = buildUrlWithContextPath("checkout");
var PULL_URL = buildUrlWithContextPath("pull");
var PUSH_URL = buildUrlWithContextPath("push");
var PULLREQUEST_URL = buildUrlWithContextPath("pullRequest");
var WORKING_COPY_URL = buildUrlWithContextPath("workingCopy");
var BRANCH_ACTIONS_URL = buildUrlWithContextPath("branchActions");
var COMMIT_URL = buildUrlWithContextPath("commit");
var WC_ACTIONS_URL = buildUrlWithContextPath("wcActions");

var CURRENT_USER_DATA;
var CURRENT_REPOSITORY_DATA;
var WORKING_COPY_LIST;
var IS_RTB;
var IS_HEAD_RTB;
var IS_UNCOMMITED_CHANGES;


$(function () {
    initializeWindow();
});

$(function () {
    setInterval(refreshCurrentUserData, 4000);
    setInterval(refresRepositoryData, 30000);

});

function initializeWindow() {
    refresRepositoryData();
    // refreshCurrentUserData is called as refresRepositoryData callbacks
}

// USER DATA
function refreshCurrentUserData() {
    ajaxCurrentUserData(function (currentUserData) {
        CURRENT_USER_DATA = currentUserData;
        $("#topBarUsername").text( CURRENT_USER_DATA.userName);
        displaySideMenuRepositories(currentUserData);
        refreshForkedRepositoriesTable();
        refreshPullRequestTable();
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
//
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
        refreshHeadlineData();
        refreshCurrentUserData();
        updateUncommitedChanges();
        displayBranchesCheckoutButtons();
        refreshCommitsTable();
        refreshWorkingCopyList();
        refreshRemoteButtons();
        refreshPullrequestForm();
    });

    function ajaxRepositoryData(callback) {
        $.ajax({
            url: CURRENT_REPOSITORY_DATA_URL,
            dataType: "json",
            success: function (currentRepositoryData) {
                callback(currentRepositoryData);
            }
        });
    }

}


function refreshHeadlineData() {
    let repoName = CURRENT_REPOSITORY_DATA.name +
    IS_RTB === true ? " (Forked from user" + CURRENT_REPOSITORY_DATA.remoteName + ")" : "";

    $("#shown-repo-headline").text(repoName);
}

function updateUncommitedChanges() {
    IS_UNCOMMITED_CHANGES = CURRENT_REPOSITORY_DATA.isUncommitedChanges;
    // TODO set all uncommited changes lables hiden or shown
}
function refreshRemoteButtons() {
    $("#new-branch-button").removeAttr('disabled').button("refresh");
    IS_RTB = CURRENT_REPOSITORY_DATA.isRTB;

    if(IS_RTB) {
        $("#pull-button").removeAttr('disabled').on("click", function () {
            pull();
        }).button("refresh");
        $("#pull-request-button").removeAttr('disabled').button("refresh");
        IS_HEAD_RTB === true ?
            $("#push-button").attr("disabled", "disabled").button("refresh") :
                $("#push-button").removeAttr('disabled').on("click", function () {
                    push();
                }).button("refresh");

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

function refreshPullRequestTable() {
    $("#pullRequestsList").empty();
    $.each(CURRENT_USER_DATA.pullRequestsDataList || [], addSinglePullRequestRow)
}

function displayBranchesCheckoutButtons() {
    $("#branchCheckoutButtons").empty();
    $.each(CURRENT_REPOSITORY_DATA.branchesDataList || [], addSingleBranchCheckoutButton);
}

function refreshForkedRepositoriesTable() {
    $("#forkedRepositoriesTable").empty();
    $.each(CURRENT_USER_DATA.forkedRepositories || [], addSingleForkedRepositoryRow)
}

function refreshPullrequestForm() {
    $("#targetBranchOptions").empty();
    $("#baseBranchOptions").empty();
    $.each(CURRENT_REPOSITORY_DATA.branchesDataList || [], addSingleBranchPRoption)
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

function addSinglePullRequestRow(index, prData) {
    if (prData.repositoryName === CURRENT_REPOSITORY_DATA.name) {
        let singlePullRequestRow = createSinglePullRequestRow(prData);
        $("#pullRequestsList").append(singlePullRequestRow);
    }
}

function addSingleBranchCheckoutButton(index, branchData) {
    let branchCheckoutButtonHTML = createBranchCheckoutButton(branchData);
    $("#branchCheckoutButtons").append(branchCheckoutButtonHTML);
}

function addSingleBranchPRoption(index, branchData) {
    let branchOptionHTML = $('<option value="' + branchData.name + '">' + branchData.name + '</option>\n')
    if (branchData.collaborationSource === "remotetracking") {
        $("#targetBranchOptions").append(branchOptionHTML);
    } else if (branchData.collaborationSource === "remote") {
        $("#baseBranchOptions").append(branchOptionHTML);
    }
}

function addSingleForkedRepositoryRow(key, value) {
    $.each(value || [] , function(index, repositoryName) {
        if(repositoryName === CURRENT_REPOSITORY_DATA.name) {
            let forkedRepoRow = createForkedRepositoryRow(key, repositoryName);
            $("#forkedRepositoriesTable").append(forkedRepoRow);
        }
    })
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

function createSinglePullRequestRow(prData) {
    let prStatusButton;
    let approveAction = "approve";
    let declineAction = "decline";

    switch(prData.status) {
        case "open":
            prStatusButton = '<div class="dropdown">'+
                             '    <button class="btn btn-secondary dropdown-toggle" type="button" id="dropdownMenuButton" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">'+
                             '        Resolve'+
                             '    </button>'+
                             '    <div class="dropdown-menu" aria-labelledby="dropdownMenuButton">'+
                             '        <a class="dropdown-item" onclick="resolvePullRequest(\'' + approveAction + '\', \'' + prData.id + '\')"><i class="fas fa-check-circle"></i> Approve</a>'+
                             '        <a class="dropdown-item" onclick="showPrDeclineModal(\'' + prData.id + '\', \'' + prData.author + '\')"><i class="fas fa-times-circle"></i> Decline</a>'+
                             '    </div>'+
                             '</div>';
            break;
        case "approved":
            prStatusButton = '<a class="btn btn-success disabled btn-icon-split" role="button">'+
                             '    <span class="text-white-50 icon">'+
                             '        <i class="fas fa-check"></i>'+
                             '    </span>'+
                             '    <span class="text-white text">Approved</span>'+
                             '</a>';
            break;
        case "declined":
            prStatusButton = '<a class="btn btn-danger disabled btn-icon-split" role="button">'+
                             '    <span class="text-white-50 icon">'+
                             '        <i class="far fa-window-close"></i>'+
                             '    </span>'+
                             '    <span class="text-white text">Declined</span>'+
                             '</a>';
            break;
    }

    let tableRow = $('<tr style="cursor: pointer;">'+
                     '    <td class="clickableTd">' + prData.author + '</td>'+
                     '    <td class="clickableTd">' + prData.targetBranch + '</td>'+
                     '    <td class="clickableTd">' + prData.baseBranch + '</td>'+
                     '    <td class="clickableTd">' + prData.description + '</td>'+
                     '    <td class="clickableTd">' + prData.date + '</td>'+
                     '    <td>'+
                          prStatusButton +
                     '    </td>' +
                     '</tr>'
    );

    tableRow.on( "click", ".clickableTd", function () {
        showPrInfoModal(prData.commitsDataList, prData.statusLogString);
    });

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
    var isRTB = branchData.collaborationSource === "remotetracking";


    if (CURRENT_REPOSITORY_DATA.activeBranchName === branchData.name) {
        // IS_HEAD_RTB = branchData.isRtb;   WRONG USAGE?
        disabled = true;
        if (isRTB) {
            btnClass = "btn btn-outline-warning";
        } else {
            btnClass = "btn btn-warning";
        }
    } else if (branchData.name === "master") {
        if (isRTB) {
            btnClass = "btn btn-outline-success"
        } else {
            btnClass = "btn btn-success";
        }
    } else {
        if (isRTB) {
            btnClass = "btn btn-outline-secondary";
        } else {
            btnClass = "btn btn-secondary";
        }
    }

    let btn = $("<button type='button'>" + branchData.name + "</button>");
    btn.on("click", function () {
        checkout(branchData.name);
        IS_HEAD_RTB = (branchData.collaborationSource === "remotetracking");
    });
    btn.addClass(btnClass);

    let deleteBtn = $("<button type='button' class='btn btn-light' name='deleteBranchBTN'><i class='far fa-trash-alt fa-sm'></i></button>");
    deleteBtn.on("click", function () {
        showDeleteBranchModal(branchData.name);
    });

    if (disabled) {
        btn.attr("disabled", "disabled").button("refresh");
        deleteBtn.attr("disabled", "disabled").button("refresh");
    }

    let buttonGroupDiv = $("<div class='btn-group mr-2' role='group' aria-label='branchBtnGroup' style='margin: 5px;'></div>");

    buttonGroupDiv.append(deleteBtn, btn);

    return buttonGroupDiv;
}

function createSingleWorkingCopyRow(componentData) {
    var icon = (componentData.type === "folder") ? "<i class=\"fas fa-folder-open\"></i>" : "<i class=\"far fa-file-alt\"></i>";
    var indentationPadding = componentData.level * 30 + 10;
    var deleteButton = (componentData.type === "folder") ? 'disabled="disabled"' :
        'onclick="deleteFile(\'' + componentData.name + '\', \'' + componentData.path + '\')" ';
    var editOnClick = 'onclick="showFileModal("edit" , \'' + componentData.path + '\', \'' + componentData.name + '\', \'' + componentData.content + '\')" ';
    var editButton = (componentData.type === "folder") ? 'disabled="disabled"' : editOnClick;       // showFileModal(action, path, name, content) TODO check editFile

    let btn = $(
        '<tr>'  +
        '   <td style="padding-left: ' + indentationPadding + 'px;">  '  +
        '       <a href="#">' +
        icon + '  ' + componentData.name +
        '       </a></td>  '  +
        '   <td class="text-center">  '  +
        '       <button class="btn btn-danger btn-circle ml-1" type="button" ' +
        deleteButton  + '>  '  +
        '           <i class="fas fa-trash text-white"></i>  '  +
        '       </button>  '  +
        '       <button class="btn btn-warning btn-circle ml-1" type="button" ' +
        editButton + '>  '  +
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

    function checkoutCallback(response) {
        if (response.success) {
            refresRepositoryData();
        } else {
            ShowModal(response);
        }
    }
}

function sendPullRequest() {
    let target = document.getElementById("targetBranchOptions").value;
    let base = document.getElementById("baseBranchOptions").value;
    let description = document.getElementById("prDescription").value;
    let author = document.getElementById("topBarUsername").innerText;

    $.ajax(
        {
            url: PULLREQUEST_URL,
            dataType: "json",
            data: {
                prAction: "send",
                prTarget: target,
                prBase: base,
                prDescription: description
            },
            success: (message) => {
                ShowModal(message)
            }
        }
    );
}



function resolvePullRequest(action, prID, author) {
    let declineReason = document.getElementById("modal-prDeclineReason-content").value;

    $.ajax(
        {
            url: PULLREQUEST_URL,
            dataType: "json",
            data: {
                prAction: action,
                prId: prID,
                prDeclineReason: declineReason,
                prAuthor: author
            },
            success: (message) => {
                ShowModal(message)
                refresRepositoryData();
            }
        }
    );
}

function pull() {
 $.ajax(
     {
         url: PULL_URL,
         dataType: "json",
         data: {
             branchToPull: CURRENT_REPOSITORY_DATA.activeBranchName
         },

         success: (message) => {
             ShowModal(message);
             refresRepositoryData();
         }
     })
 }

 function push() {
        $.ajax(
            {
                url: PUSH_URL,
                dataType: "json",
                data:{
                    branchToPush: CURRENT_REPOSITORY_DATA.activeBranchName
                },
                success: (message) => {
                    if(message.success) {
                        ShowModal(message);
                        refresRepositoryData();
                    }
                }
            }
        )
 }

function showDeleteBranchModal(branchName) {
    ShowYesNoModal("Delete branch", "Are you sure you want to delete \"" + branchName + "\" branch?", deleteBranch(branchName), true);
}

function deleteBranch(branchName) {
    $.ajax(
        {
            url: BRANCH_ACTIONS_URL,
            dataType: "json",
            data:{
                branchAction: "delete",
                branchName: branchName
            },
            success: (message) => {
                ShowModal(message);
                refresRepositoryData();
            }
        }
    )
}

function createNewBranch() {
    let newBranchName = document.getElementById("newBranchNameInput").value;
    let shouldCheckout = document.getElementById("checkoutNewBranch").checked;
    let action = (shouldCheckout)? "createAndCheckout" : "create";

    $.ajax(
        {
            url: BRANCH_ACTIONS_URL,
            dataType: "json",
            data: {
                branchAction: action,
                branchName: newBranchName
            },
            success: (message) => {
                ShowModal(message);
                refresRepositoryData();
            }
        }
    );
}

function commit() {
    let commitDescription = document.getElementById("commitDescrition").value

    $.ajax(
        {
            url: COMMIT_URL,
            dataType: "json",
            data: {
                commitDescription: commitDescription
            },
            success: (message) => {
                console.log("on success");
                ShowModal(message);
                refresRepositoryData();
                //todo MODAL: CHECK why not showing
            }
        }
    );
}

//  working copy
function addNewFile() {
    const fileName = document.getElementById("fileModal-fileName").value;
    const fileContent = document.getElementById("fileModal-fileContent").value;

    $.ajax(
        {
            url: WC_ACTIONS_URL,
            dataType: "json",
            data: {
                wcAction: "add",
                fileName: fileName,
                fileContent: fileContent
            },
            success: (message) => {
                ShowModal(message);
                refresRepositoryData();
            }
        }
    );
}

function deleteFile(fileName, filePath) {
    $.ajax(
        {
            url: WC_ACTIONS_URL,
            dataType: "json",
            data: {
                wcAction: "delete",
                fileName: fileName,
                filePath: filePath
            },
            success: (message) => {
                ShowModal(message);
                refresRepositoryData();
            }
        }
    );
}

function editFile(filePath) {   // TODO editFile: send this function from edit modal!!
    const fileName = document.getElementById("fileModal-fileName").value;
    const fileContent = document.getElementById("fileModal-fileContent").value;

    $.ajax(
        {
            url: WC_ACTIONS_URL,
            dataType: "json",
            data: {
                wcAction: "edit",
                fileName: fileName,
                fileContent: fileContent,
                filePath: filePath
            },
            success: (message) => {
                ShowModal(message);
                refresRepositoryData();
            }
        }
    );
}

function showFileModal(action, path, name, content) {
    let acceptButton = document.getElementById("fileModalAcceptButton");
    let nameField = document.getElementById("fileModal-fileName");
    let contentField = document.getElementById("fileModal-fileContent");
    let modalTitle = document.getElementById("modal-file-title");

    if(action === "edit") {
        modalTitle.html("Edit file");

        $(nameField).attr("disabled", "disabled").button("refresh");
        $(nameField).val(name);
        $(contentField).val(content);

        acceptButton.html("Save");
        acceptButton.click(editFile(path));
    } else {
        modalTitle.html("Create new file");

        $(acceptButton).on("click", function() {
            if($(nameField).val() === "") {
                alert("File Name has to be provided in order to set this file")
            } else {
                addNewFile();
            }
        });
        acceptButton.html("Create");
    }

    $('#fileModal').modal('show');
}




function showPrDeclineModal(prID, author) {
    $('#declinePrModalButton').on( "click", function () {
        resolvePullRequest("decline", prID, author);
        $('#prDeclineReasonModal').modal('hide');
    });
    $('#prDeclineReasonModal').modal('show');
}

function showPrInfoModal(commitsDataList, statusLogString) {
    $("#prStatusLog").empty();
    $("#cmmitsDeltaList").empty();

    if (commitsDataList.length === 0) {
        let warningMessage = '<div class="alert alert-danger" role="alert">' +
                             'Base branch is not included in target branch' +
                             '</div>';
        $("#cmmitsDeltaList").append(warningMessage);
    } else {
        $("#prStatusLog").html(statusLogString.replace(/(?:\r\n|\r|\n)/g, '<br />'));
        $.each(commitsDataList || [], addSingleCommitToPrListModal);
    }

    $('#prCommitsDeltaModal').modal('show');
}

function addSingleCommitToPrListModal(index, commitData) {
    let singlePrCommitDeltaRow = createSinglePrCommitDeltaRow(commitData);
    $("#cmmitsDeltaList").append(singlePrCommitDeltaRow);
}

function createSinglePrCommitDeltaRow(commitData) {
    let commitDeltaRow = '<div class="alert alert-info" role="alert">'+
        commitData.SHA1 +
        '    <br /> '+
        commitData.message+
        '</div>';
    return commitDeltaRow;
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


function replaceSpacesWithUndersore(str){
    return str !== undefined ? str.replace(/ /g,"_") : str;
}