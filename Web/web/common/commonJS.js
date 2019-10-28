function ShowModal(response) {
    if (response.success) {
        document.getElementById("modal-success-content").textContent = response.message;
        $('#successModal').modal('show');
    } else {
        document.getElementById("modal-failure-content").textContent = response.message;
        $('#failureModal').modal('show');
    }
}

function ShowYesNoModal(title, content, yesFunctionCallback, yesDanger) {
    document.getElementById("modal-yesno-title").textContent = title;
    document.getElementById("modal-yesno-content").textContent = content;

    let yesBtn = document.getElementById("modal-yes-btn");
    yesBtn.onclick = yesFunctionCallback;
    if (yesDanger) {
        yesBtn.className = 'btn btn-danger';
    } else {
        yesBtn.className = 'btn btn-success';
    }

    $('#yesNoModal').modal('show');
}

