document.addEventListener('submit', function (event) {
    var form = event.target;

    var message = form.getAttribute('data-confirm');
    if (message && !window.confirm(message)) {
        event.preventDefault();
        return;
    }

    var busyText = form.getAttribute('data-busy-text');
    if (busyText) {
        var button = form.querySelector('button[type="submit"]');
        if (button) {
            button.textContent = busyText;
            button.disabled = true;
        }
    }
});

// Geri tuşuyla dönülen sayfa bellekten gelirse "Üretiliyor…" yazısı takılı kalmasın.
window.addEventListener('pageshow', function (event) {
    if (event.persisted) {
        window.location.reload();
    }
});