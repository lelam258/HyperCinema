(function () {
    function setOpen(dialog, isOpen) {
        if (!dialog) return;
        dialog.classList.toggle('is-open', isOpen);
        dialog.classList.toggle('show', isOpen);
        dialog.setAttribute('aria-hidden', String(!isOpen));
        if (isOpen) {
            const focusTarget = dialog.querySelector('[data-dialog-focus], button, input, select, textarea, a[href]');
            if (focusTarget) focusTarget.focus();
        }
    }

    window.HyperCinemaDialog = {
        open(id) {
            setOpen(document.getElementById(id), true);
        },
        close(id) {
            setOpen(document.getElementById(id), false);
        }
    };

    document.addEventListener('click', (event) => {
        const openButton = event.target.closest('[data-dialog-open]');
        if (openButton) {
            event.preventDefault();
            window.HyperCinemaDialog.open(openButton.dataset.dialogOpen);
        }

        const closeButton = event.target.closest('[data-dialog-close]');
        if (closeButton) {
            event.preventDefault();
            const dialog = closeButton.closest('[data-dialog], .modal-overlay, .hc-dialog');
            if (dialog) setOpen(dialog, false);
        }

        const dialogBackdrop = event.target.matches('[data-dialog].is-open, .modal-overlay.show');
        if (dialogBackdrop) {
            setOpen(event.target, false);
        }
    });

    document.addEventListener('keydown', (event) => {
        if (event.key !== 'Escape') return;
        document.querySelectorAll('[data-dialog].is-open, .modal-overlay.show').forEach((dialog) => {
            setOpen(dialog, false);
        });
    });
})();
