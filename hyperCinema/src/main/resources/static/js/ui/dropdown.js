(function () {
    let activeMenu = null;

    function close(menu) {
        if (!menu) return;
        menu.classList.remove('is-open', 'show');
        const button = document.querySelector(`[aria-controls="${menu.id}"]`);
        if (button) {
            button.setAttribute('aria-expanded', 'false');
        }
    }

    function open(menu, button) {
        if (activeMenu && activeMenu !== menu) {
            close(activeMenu);
        }
        menu.classList.add('is-open', 'show');
        button.setAttribute('aria-expanded', 'true');
        activeMenu = menu;
    }

    document.addEventListener('click', (event) => {
        const button = event.target.closest('[data-dropdown-toggle]');
        if (button) {
            const menuId = button.getAttribute('aria-controls') || button.dataset.dropdownToggle;
            const menu = document.getElementById(menuId);
            if (!menu) return;
            event.preventDefault();
            if (menu.classList.contains('is-open') || menu.classList.contains('show')) {
                close(menu);
                activeMenu = null;
            } else {
                open(menu, button);
            }
            return;
        }

        if (activeMenu && !event.target.closest('[data-hc-dropdown-menu], .dropdown-menu')) {
            close(activeMenu);
            activeMenu = null;
        }
    });

    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape' && activeMenu) {
            close(activeMenu);
            activeMenu = null;
        }
    });
})();
