(function () {
    document.querySelectorAll('[data-voucher-form]').forEach((form) => {
        const toggle = form.querySelector('[data-voucher-branch-toggle]');
        const field = form.querySelector('[data-voucher-branch-field]');
        if (!toggle || !field) return;

        function sync() {
            field.classList.toggle('hidden', !toggle.checked);
        }

        toggle.addEventListener('change', sync);
        sync();
    });
})();
