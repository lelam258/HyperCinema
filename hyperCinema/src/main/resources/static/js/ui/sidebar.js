(function () {
    const sidebar = document.querySelector('[data-sidebar]');
    const toggles = document.querySelectorAll('[data-sidebar-toggle]');
    const backdrop = document.querySelector('[data-sidebar-backdrop]');

    if (!sidebar || toggles.length === 0) {
        return;
    }

    function setOpen(isOpen) {
        sidebar.classList.toggle('is-open', isOpen);
        document.body.classList.toggle('sidebar-open', isOpen);
        if (backdrop) {
            backdrop.classList.toggle('is-open', isOpen);
        }
        toggles.forEach((toggle) => {
            toggle.setAttribute('aria-expanded', String(isOpen));
        });
    }

    toggles.forEach((toggle) => {
        toggle.addEventListener('click', () => {
            setOpen(!sidebar.classList.contains('is-open'));
        });
    });

    if (backdrop) {
        backdrop.addEventListener('click', () => setOpen(false));
    }

    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape') {
            setOpen(false);
        }
    });
})();
