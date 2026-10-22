document.addEventListener('input', function (event) {
    var field = event.target.closest('.field');
    if (field) {
        field.querySelectorAll('.field-error').forEach(function (el) {
            el.remove();
        });
    }
    document.querySelectorAll('[data-clear-on-input]').forEach(function (el) {
        el.remove();
    });

    document.addEventListener('change', function (event) {
        var form = event.target.closest('form[data-auto-submit]');
        if (form) {
            form.submit();
        }
    });
});