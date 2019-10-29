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

