(function () {
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';

    function headers(extra) {
        const base = Object.assign({ 'Content-Type': 'application/json' }, extra || {});
        if (csrfToken) base[csrfHeader] = csrfToken;
        return base;
    }

    function modal(name) {
        return document.querySelector('[data-user-modal="' + name + '"]');
    }

    function openModal(name) {
        const target = modal(name);
        if (!target) return;
        target.classList.remove('hidden');
        target.classList.add('flex');
        const firstInput = target.querySelector('input:not([type="hidden"]), select, button');
        if (firstInput) firstInput.focus();
    }

    function closeModal(target) {
        const wrapper = target?.closest?.('[data-user-modal]') || target;
        if (!wrapper) return;
        wrapper.classList.add('hidden');
        wrapper.classList.remove('flex');
    }

    function toast(message, type) {
        const container = document.querySelector('[data-user-toasts]');
        if (!container) return;
        const node = document.createElement('div');
        node.className = 'rounded-lg border px-4 py-3 text-sm shadow-xl ' +
            (type === 'danger'
                ? 'border-danger/40 bg-danger/15 text-red-100'
                : 'border-success/40 bg-success/15 text-green-100');
        node.textContent = message;
        container.appendChild(node);
        setTimeout(() => node.remove(), 3500);
    }

    async function requestJson(url, options) {
        const response = await fetch(url, options);
        if (response.ok) return response;
        let message = 'Thao tác không thành công';
        try {
            const body = await response.json();
            message = body.message || message;
        } catch (ignored) {
            message = response.statusText || message;
        }
        throw new Error(message);
    }

    function fillEditForm(button) {
        const form = modal('edit')?.querySelector('[data-user-form="edit"]');
        if (!form) return;
        form.elements.id.value = button.dataset.userId || '';
        form.elements.name.value = button.dataset.userName || '';
        form.elements.username.value = button.dataset.userUsername || '';
        form.elements.email.value = button.dataset.userEmail || '';
        form.elements.phone.value = button.dataset.userPhone || '';
        form.elements.roleId.value = button.dataset.userRole || '';
        form.elements.status.value = button.dataset.userStatus || 'Active';
    }

    function fillIdForm(name, button) {
        const form = modal(name)?.querySelector('[data-user-form="' + name + '"]');
        if (!form) return;
        form.elements.id.value = button.dataset.userId || '';
        if (form.elements.roleId) form.elements.roleId.value = button.dataset.userRole || '';
        if (form.elements.password) form.elements.password.value = '';
    }

    document.addEventListener('click', async (event) => {
        const closeButton = event.target.closest('[data-user-close]');
        if (closeButton) {
            closeModal(closeButton);
            return;
        }

        const opener = event.target.closest('[data-user-open]');
        if (opener) {
            const name = opener.dataset.userOpen;
            if (name === 'edit') fillEditForm(opener);
            if (name === 'role' || name === 'reset') fillIdForm(name, opener);
            openModal(name);
            return;
        }

        const backdrop = event.target.matches('[data-user-modal]') ? event.target : null;
        if (backdrop) {
            closeModal(backdrop);
            return;
        }

        const action = event.target.closest('[data-user-action]');
        if (!action) return;
        const userId = action.dataset.userId;
        if (!userId) return;

        try {
            if (action.dataset.userAction === 'toggle') {
                await requestJson('/api/admin/users/' + userId + '/toggle-status', {
                    method: 'POST',
                    headers: headers()
                });
                toast('Đã cập nhật trạng thái người dùng');
                setTimeout(() => window.location.reload(), 700);
            }

            if (action.dataset.userAction === 'delete' && confirm('Xóa vĩnh viễn người dùng này?')) {
                await requestJson('/api/admin/users/' + userId, {
                    method: 'DELETE',
                    headers: headers()
                });
                toast('Đã xóa người dùng');
                setTimeout(() => window.location.reload(), 700);
            }
        } catch (error) {
            toast(error.message, 'danger');
        }
    });

    document.querySelectorAll('[data-user-form]').forEach((form) => {
        form.addEventListener('submit', async (event) => {
            event.preventDefault();
            const type = form.dataset.userForm;
            const data = Object.fromEntries(new FormData(form).entries());

            try {
                if (type === 'add') {
                    await requestJson('/api/admin/users', {
                        method: 'POST',
                        headers: headers(),
                        body: JSON.stringify({
                            name: data.name,
                            username: data.username,
                            email: data.email,
                            password: data.password,
                            phone: data.phone,
                            roleId: Number(data.roleId),
                            status: 'Active'
                        })
                    });
                    toast('Đã tạo người dùng');
                }

                if (type === 'edit') {
                    await requestJson('/api/admin/users/' + data.id, {
                        method: 'PUT',
                        headers: headers(),
                        body: JSON.stringify({
                            name: data.name,
                            username: data.username,
                            email: data.email,
                            phone: data.phone,
                            roleId: Number(data.roleId),
                            status: data.status
                        })
                    });
                    toast('Đã cập nhật người dùng');
                }

                if (type === 'role') {
                    await requestJson('/api/admin/users/' + data.id + '/assign-role', {
                        method: 'POST',
                        headers: headers(),
                        body: JSON.stringify(Number(data.roleId))
                    });
                    toast('Đã cập nhật vai trò');
                }

                if (type === 'reset') {
                    await requestJson('/api/admin/users/' + data.id + '/reset-password', {
                        method: 'POST',
                        headers: headers(),
                        body: JSON.stringify({ password: data.password })
                    });
                    toast('Đã đặt lại mật khẩu');
                }

                closeModal(form);
                setTimeout(() => window.location.reload(), 700);
            } catch (error) {
                toast(error.message, 'danger');
            }
        });
    });

    document.addEventListener('keydown', (event) => {
        if (event.key !== 'Escape') return;
        document.querySelectorAll('[data-user-modal].flex').forEach(closeModal);
    });
})();
