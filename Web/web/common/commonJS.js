var NOTIFICATIONS_CENTER;
var NOTIFICATIONS_URL = buildUrlWithContextPath("notifications");

$(function () {
    setInterval(refreshNotifications, 2000);
});

function refreshNotifications() {
    ajaxRefreshNotifications( function (notificationsCenter) {
        NOTIFICATIONS_CENTER = notificationsCenter;
        createNotificationsHTML();
    });
}

function ajaxRefreshNotifications(callback) {
    $.ajax({
        url: NOTIFICATIONS_URL,
        dataType: "json",
        data: {
            notificationsAction: "get"
        },
        success: function (notificationsCenter) {
            callback(notificationsCenter);
        }
    });
}

function seenNotifications() {
    ajaxSeenNotifications(() => {
        refreshNotifications();
    });
}

function ajaxSeenNotifications(callback) {
    $.ajax({
        url: NOTIFICATIONS_URL,
        dataType: "json",
        data: {
            notificationsAction: "see"
        },
        success: function () {
            callback();
        }
    });
}

function createNotificationsHTML() {
    $("#notificationsList").empty();
    $.each(NOTIFICATIONS_CENTER.notifications || [], addSingleNotification);
    updateNotificationsBadge();
}

function addSingleNotification(index, notificationData) {
    let notification = createSingleNotificationHTML(notificationData);
    $("#notificationsList").append(notification);
}

function createSingleNotificationHTML(notificationData) {
    let notification;

    switch(notificationData.type) {
        case "pr":
            notification = '<a class="d-flex align-items-center dropdown-item" href="#">'+
                           '    <div class="mr-3">'+
                           '        <div class="bg-primary icon-circle">'+
                           '            <i class="far fa-window-restore text-white"></i>'+
                           '        </div>'+
                           '    </div>'+
                           '    <div><span class="small text-gray-500">' + notificationData.date + '</span>'+
                           '        <p>' + notificationData.content + '</p>'+
                           '    </div>'+
                           '</a>';
            break;
        case "fork":
            notification = '<a class="d-flex align-items-center dropdown-item" href="#">'+
                           '    <div class="mr-3">'+
                           '        <div class="bg-success icon-circle">'+
                           '            <i class="fas fa-code-branch text-white"></i>'+
                           '        </div>'+
                           '    </div>'+
                           '    <div><span class="small text-gray-500">' + notificationData.date + '</span>'+
                           '        <p>' + notificationData.content + '</p>'+
                           '    </div>'+
                           '</a>';

            ;
            break;
        case "alert":
            notification = '<a class="d-flex align-items-center dropdown-item" href="#">'+
                           '    <div class="mr-3">'+
                           '        <div class="bg-warning icon-circle">'+
                           '            <i class="fas fa-eraser text-white"></i>'+
                           '        </div>'+
                           '    </div>'+
                           '    <div><span class="small text-gray-500">' + notificationData.date + '</span>'+
                           '        <p>' + notificationData.content + '</p>'+
                           '    </div>'+
                           '</a>';
            break;
    }

    return notification;
}

function updateNotificationsBadge() {
    var unseenNotifications = NOTIFICATIONS_CENTER.unseen;
    console.log("1" + unseenNotifications);

    if (unseenNotifications === 0) {
        console.log("2", unseenNotifications);
        $('#notificationsBadge').hide();
    } else {
        $('#notificationsBadge').show();
    }
    $('#notificationsBadge').text(unseenNotifications);
}







function ShowModal(response) {
    var json = (response.success === undefined)? JSON.parse(response) : response;

    if (json.success) {
        document.getElementById("modal-success-content").textContent = json.message;
        $('#successModal').modal('show');
    } else {
        document.getElementById("modal-failure-content").textContent = json.message;
        $('#failureModal').modal('show');
    }
}

function ShowYesNoModal(title, content, yesFunctionCallback, yesDanger) {
    document.getElementById("modal-yesno-title").textContent = title;
    document.getElementById("modal-yesno-content").textContent = content;

    let yesBtn = document.getElementById("modal-yes-btn");
    yesBtn.click(yesFunctionCallback);

    if (yesDanger) {
        yesBtn.className = 'btn btn-danger';
    } else {
        yesBtn.className = 'btn btn-success';
    }

    $('#yesNoModal').modal('show');
}


