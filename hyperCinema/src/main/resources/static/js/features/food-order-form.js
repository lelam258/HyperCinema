(function () {
    function reindex(container) {
        container.querySelectorAll('[data-food-item-row]').forEach((row, index) => {
            const select = row.querySelector('select');
            const quantity = row.querySelector('input[type="number"]');
            if (select) select.name = 'items[' + index + '].itemId';
            if (quantity) quantity.name = 'items[' + index + '].quantity';
        });
    }

    document.querySelectorAll('[data-food-order-form]').forEach((form) => {
        const container = form.querySelector('[data-food-item-container]');
        const template = form.querySelector('[data-food-item-template]');
        const addButton = form.querySelector('[data-food-item-add]');
        if (!container || !template || !addButton) return;

        addButton.addEventListener('click', () => {
            const fragment = template.content.cloneNode(true);
            const row = fragment.querySelector('[data-food-item-row]');
            container.appendChild(fragment);
            if (row) reindex(container);
        });

        form.addEventListener('click', (event) => {
            const removeButton = event.target.closest('[data-food-item-remove]');
            if (!removeButton) return;
            const rows = container.querySelectorAll('[data-food-item-row]');
            if (rows.length <= 1) return;
            removeButton.closest('[data-food-item-row]')?.remove();
            reindex(container);
        });

        reindex(container);
    });
})();
