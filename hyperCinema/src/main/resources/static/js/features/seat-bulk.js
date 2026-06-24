(function () {
    function rowTemplate(index) {
        const row = document.createElement('div');
        row.className = 'hc-bulk-row';
        row.dataset.seatRow = '';
        row.innerHTML = [
            '<input class="hc-input" type="text" name="rows[' + index + '].rowLabel" placeholder="Nhan hang (VD: A)" maxlength="5" required>',
            '<input class="hc-input" type="number" name="rows[' + index + '].seatCount" placeholder="So ghe" min="1" required>',
            '<button type="button" class="hc-icon-button hc-icon-danger" title="Xoa hang" data-seat-row-remove><i data-lucide="trash-2"></i><span class="sr-only">Xoa hang</span></button>'
        ].join('');
        return row;
    }

    function reindex(container) {
        container.querySelectorAll('[data-seat-row]').forEach((row, index) => {
            const inputs = row.querySelectorAll('input');
            if (inputs[0]) inputs[0].name = 'rows[' + index + '].rowLabel';
            if (inputs[1]) inputs[1].name = 'rows[' + index + '].seatCount';
        });
    }

    document.querySelectorAll('[data-seat-bulk-form]').forEach((form) => {
        const container = form.querySelector('[data-seat-row-container]');
        const addButton = form.querySelector('[data-seat-row-add]');
        if (!container || !addButton) return;

        if (container.querySelectorAll('[data-seat-row]').length === 0) {
            container.appendChild(rowTemplate(0));
        }

        addButton.addEventListener('click', () => {
            const index = container.querySelectorAll('[data-seat-row]').length;
            container.appendChild(rowTemplate(index));
            if (window.lucide) window.lucide.createIcons();
        });

        form.addEventListener('click', (event) => {
            const removeButton = event.target.closest('[data-seat-row-remove]');
            if (!removeButton) return;
            const row = removeButton.closest('[data-seat-row]');
            if (!row) return;
            row.remove();
            if (container.querySelectorAll('[data-seat-row]').length === 0) {
                container.appendChild(rowTemplate(0));
            }
            reindex(container);
            if (window.lucide) window.lucide.createIcons();
        });
    });
})();
