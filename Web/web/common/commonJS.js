function ShowModal(response) {
    if (response.success) {
        document.getElementById("modal-success-content").textContent = response.message;
        $('#successModal').modal('show');
    } else {
        document.getElementById("modal-failure-content").textContent = response.message;
        $('#failureModal').modal('show');
    }
}