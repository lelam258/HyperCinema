(function () {
    const TYPE_BADGE_CLASSES = ['hc-badge-accent', 'hc-badge-pink', 'hc-badge-teal'];
    const TYPE_LABELS = {
        STANDARD: 'STD',
        VIP: 'VIP',
        COUPLE: 'CPL',
        DISABLED: 'DIS'
    };
    const TYPE_CLASSES = {
        VIP: 'hc-badge-accent',
        COUPLE: 'hc-badge-pink',
        DISABLED: 'hc-badge-teal'
    };

    function onReady(callback) {
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', callback, { once: true });
            return;
        }
        callback();
    }

    function normalizeSeatType(type) {
        const value = String(type || 'STANDARD').trim().toUpperCase().replace(/\s+/g, '_');
        if (value === 'STD') return 'STANDARD';
        if (value === 'CPL') return 'COUPLE';
        if (value === 'DIS') return 'DISABLED';
        return TYPE_LABELS[value] ? value : 'STANDARD';
    }

    function normalizeMaintenanceStatus(status) {
        const value = String(status || 'AVAILABLE').trim().toUpperCase().replace(/\s+/g, '_');
        return value === 'UNDER_MAINTENANCE' ? value : 'AVAILABLE';
    }

    function setBadge(badge, type) {
        if (!badge) return;
        const normalizedType = normalizeSeatType(type);
        TYPE_BADGE_CLASSES.forEach((className) => badge.classList.remove(className));
        const badgeClass = TYPE_CLASSES[normalizedType];
        if (badgeClass) {
            badge.classList.add(badgeClass);
        }
        badge.textContent = TYPE_LABELS[normalizedType] || '--';
    }

    function setAction(form, url) {
        if (form && url) {
            form.action = url;
        }
    }

    function initSeatMap(root) {
        const cards = Array.from(root.querySelectorAll('[data-seat-card]'));
        const empty = root.querySelector('[data-seat-empty]');
        const body = root.querySelector('[data-seat-editor-body]');
        const clearButton = root.querySelector('[data-seat-clear]');
        const editor = root.querySelector('[data-seat-editor]');
        const label = root.querySelector('[data-seat-editor-label]');
        const badge = root.querySelector('[data-seat-editor-badge]');
        const updateForm = root.querySelector('[data-seat-update-form]');
        const maintenanceForm = root.querySelector('[data-seat-maintenance-form]');
        const maintenanceInput = root.querySelector('[data-seat-maintenance-status]');
        const maintenanceText = root.querySelector('[data-seat-maintenance-text]');
        const deleteForm = root.querySelector('[data-seat-delete-form]');
        const deleteButton = root.querySelector('[data-seat-delete-button]');
        const deleteNote = root.querySelector('[data-seat-delete-note]');
        const fields = {
            seatRow: root.querySelector('[data-seat-field="seatRow"]'),
            seatNumber: root.querySelector('[data-seat-field="seatNumber"]'),
            type: root.querySelector('[data-seat-field="type"]'),
            maintenanceStatus: root.querySelector('[data-seat-field="maintenanceStatus"]')
        };
        let selectedCard = null;

        function clearSelection() {
            selectedCard = null;
            cards.forEach((card) => {
                card.classList.remove('is-selected');
                card.setAttribute('aria-pressed', 'false');
            });
            if (empty) empty.hidden = false;
            if (body) body.hidden = true;
            if (editor) editor.hidden = true;
        }

        function selectSeat(card) {
            selectedCard = card;
            cards.forEach((item) => {
                const isSelected = item === card;
                item.classList.toggle('is-selected', isSelected);
                item.setAttribute('aria-pressed', String(isSelected));
            });

            const seatType = normalizeSeatType(card.dataset.seatType);
            const maintenanceStatus = normalizeMaintenanceStatus(card.dataset.seatMaintenance);
            const nextMaintenanceStatus = normalizeMaintenanceStatus(card.dataset.seatNextMaintenance
                || (maintenanceStatus === 'UNDER_MAINTENANCE' ? 'AVAILABLE' : 'UNDER_MAINTENANCE'));
            const hasActiveReference = card.dataset.seatHasActiveReference === 'true';

            if (empty) empty.hidden = true;
            if (body) body.hidden = false;
            if (editor) editor.hidden = false;
            if (label) label.textContent = card.dataset.seatLabel || '--';
            setBadge(badge, seatType);

            if (fields.seatRow) fields.seatRow.value = card.dataset.seatRow || '';
            if (fields.seatNumber) fields.seatNumber.value = card.dataset.seatNumber || '';
            if (fields.type) fields.type.value = seatType;
            if (fields.maintenanceStatus) fields.maintenanceStatus.value = maintenanceStatus;

            setAction(updateForm, card.dataset.seatUpdateUrl);
            setAction(maintenanceForm, card.dataset.seatMaintenanceUrl);
            setAction(deleteForm, card.dataset.seatDeleteUrl);

            if (maintenanceInput) {
                maintenanceInput.value = nextMaintenanceStatus;
            }
            if (maintenanceText) {
                maintenanceText.textContent = nextMaintenanceStatus === 'AVAILABLE'
                    ? 'Bỏ bảo trì'
                    : 'Đánh dấu bảo trì';
            }
            if (deleteButton) {
                deleteButton.disabled = hasActiveReference;
                deleteButton.classList.toggle('is-disabled', hasActiveReference);
                deleteButton.title = hasActiveReference ? 'Khong the xoa vi ghe dang duoc su dung' : 'Xoa ghe';
            }
            if (deleteNote) {
                deleteNote.hidden = !hasActiveReference;
            }
        }

        cards.forEach((card) => {
            card.setAttribute('aria-pressed', 'false');
            card.addEventListener('click', () => selectSeat(card));
        });

        if (clearButton) {
            clearButton.addEventListener('click', clearSelection);
        }

        [updateForm, maintenanceForm, deleteForm].forEach((form) => {
            if (!form) return;
            form.addEventListener('submit', (event) => {
                if (!selectedCard) {
                    event.preventDefault();
                }
            });
        });

        if (fields.type) {
            fields.type.addEventListener('change', () => setBadge(badge, fields.type.value));
        }

        clearSelection();
    }

    onReady(() => {
        document.querySelectorAll('[data-seat-admin-map]').forEach(initSeatMap);
    });
})();
