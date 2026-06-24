(function () {
    window.HyperCinemaToast = function showToast(message, type) {
        const containerId = 'hc-toast-container';
        let container = document.getElementById(containerId);
        if (!container) {
            container = document.createElement('div');
            container.id = containerId;
            container.className = 'fixed bottom-6 right-6 z-[2000] flex max-w-sm flex-col gap-3';
            document.body.appendChild(container);
        }

        const toast = document.createElement('div');
        toast.className = `hc-toast ${type ? `hc-toast-${type}` : ''}`;
        toast.textContent = message;
        container.appendChild(toast);

        setTimeout(() => {
            toast.remove();
        }, 4200);
    };
})();
