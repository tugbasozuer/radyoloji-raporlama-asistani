(function () {
    var KEY = 'listPage';

    function isLocalPath(value) {
        return typeof value === 'string' && value.charAt(0) === '/' && value.charAt(1) !== '/';
    }

    try {
        if (document.body.hasAttribute('data-remember-page')) {
            sessionStorage.setItem(KEY, window.location.pathname + window.location.search);
        }
        var saved = sessionStorage.getItem(KEY);
        if (isLocalPath(saved)) {
            document.querySelectorAll('[data-back-to-list]').forEach(function (link) {
                link.setAttribute('href', saved);
            });
            document.querySelectorAll('input[name="geri"]').forEach(function (input) {
                input.value = saved;
            });
        }
    } catch (e) {
        // sessionStorage kapalıysa bağlantılar varsayılan adresle kalır.
    }
})();