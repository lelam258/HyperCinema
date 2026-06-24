(function () {
    function formatCurrency(value) {
        return new Intl.NumberFormat('vi-VN').format(value || 0);
    }

    function recalculate(root) {
        const rows = root.querySelectorAll('[data-pos-item]');
        let total = 0;
        rows.forEach((row) => {
            const price = Number(row.dataset.price || 0);
            const quantityInput = row.querySelector('[data-pos-qty]');
            const quantity = Number(quantityInput ? quantityInput.value : row.dataset.qty || 1);
            total += price * quantity;
        });

        root.querySelectorAll('[data-pos-total]').forEach((node) => {
            node.textContent = formatCurrency(total);
        });
    }

    document.addEventListener('input', (event) => {
        if (!event.target.matches('[data-pos-qty]')) return;
        const root = event.target.closest('[data-pos-root]');
        if (root) recalculate(root);
    });

    document.querySelectorAll('[data-pos-root]').forEach(recalculate);
})();
