(() => {
    const currency = (value) => new Intl.NumberFormat("vi-VN").format(Math.max(0, Number(value) || 0)) + " VND";

    const rowIndexFor = (row, rows) => {
        const singleLetters = rows.every((value) => /^[A-Za-z]$/.test(value));
        if (!singleLetters) return rows.indexOf(row) + 1;
        const base = Math.min(...rows.map((value) => value.toUpperCase().charCodeAt(0)));
        return row.toUpperCase().charCodeAt(0) - base + 1;
    };

    document.querySelectorAll("[data-staff-pos-root]").forEach((root) => {
        if (root.dataset.staffPosReady === "true") return;
        root.dataset.staffPosReady = "true";

        const form = root.querySelector("[data-staff-pos-form]");
        const tabButtons = Array.from(root.querySelectorAll("[data-pos-tab]"));
        const panes = Array.from(root.querySelectorAll("[data-pos-pane]"));
        const timeButtons = Array.from(root.querySelectorAll("[data-pos-showtime-id]"));
        const modal = root.querySelector("[data-pos-seat-modal]");
        const modalTitle = root.querySelector("[data-pos-modal-title]");
        const modalMeta = root.querySelector("[data-pos-modal-meta]");
        const modalSeatCount = root.querySelector("[data-pos-modal-seat-count]");
        const modalConfirm = root.querySelector("[data-pos-modal-confirm]");
        const seatGrid = root.querySelector("[data-pos-seat-grid]");
        const seatEmpty = root.querySelector("[data-pos-seat-empty]");
        const showtimeInput = root.querySelector("[data-pos-showtime-input]");
        const paymentInput = root.querySelector("[data-pos-payment-input]");
        const voucherInput = root.querySelector("[data-pos-voucher-input]");
        const voucherCodeInput = root.querySelector("[data-pos-voucher-code]");
        const voucherMessage = root.querySelector("[data-pos-voucher-message]");
        const customerPhone = root.querySelector("[data-pos-customer-phone]");
        const customerMessage = root.querySelector("[data-pos-customer-message]");
        const cartEmpty = root.querySelector("[data-pos-cart-empty]");
        const cartList = root.querySelector("[data-pos-cart-list]");
        const subtotalNode = root.querySelector("[data-pos-subtotal]");
        const discountNode = root.querySelector("[data-pos-discount]");
        const totalNode = root.querySelector("[data-pos-total]");
        const submitButton = root.querySelector("[data-pos-submit]");
        const selectedSeatsNode = root.querySelector("[data-pos-selected-seats]");
        const selectedFoodNode = root.querySelector("[data-pos-selected-food]");
        const paymentButtons = Array.from(root.querySelectorAll("[data-pos-payment]"));
        const foodItems = Array.from(root.querySelectorAll("[data-pos-food-id]"));
        const seatCache = new Map();

        const state = {
            showtime: null,
            seats: [],
            food: new Map(),
            paymentMethod: "",
            voucher: null,
        };

        const modalState = {
            showtime: null,
            seats: [],
            loading: false,
        };

        const sameShowtime = (showtime) => String(state.showtime?.id || "") === String(showtime?.id || "");

        const clearVoucher = () => {
            state.voucher = null;
            if (voucherCodeInput) voucherCodeInput.value = "";
            if (voucherMessage) voucherMessage.textContent = "";
        };

        const selectedSubtotal = () => {
            const seatTotal = state.seats.reduce((sum, seat) => sum + Number(seat.price || 0), 0);
            const foodTotal = Array.from(state.food.values())
                .reduce((sum, item) => sum + Number(item.price || 0) * Number(item.quantity || 0), 0);
            return seatTotal + foodTotal;
        };

        const syncHiddenInputs = () => {
            if (showtimeInput) showtimeInput.value = state.showtime?.id || "";
            if (paymentInput) paymentInput.value = state.paymentMethod || "";
            if (selectedSeatsNode) {
                selectedSeatsNode.replaceChildren();
                state.seats.forEach((seat) => {
                    const input = document.createElement("input");
                    input.type = "hidden";
                    input.name = "seatIds";
                    input.value = seat.id;
                    selectedSeatsNode.appendChild(input);
                });
            }
            if (selectedFoodNode) {
                selectedFoodNode.replaceChildren();
                state.food.forEach((item) => {
                    const idInput = document.createElement("input");
                    idInput.type = "hidden";
                    idInput.name = "foodItemIds";
                    idInput.value = item.id;
                    const quantityInput = document.createElement("input");
                    quantityInput.type = "hidden";
                    quantityInput.name = "foodQuantities";
                    quantityInput.value = item.quantity;
                    selectedFoodNode.append(idInput, quantityInput);
                });
            }
        };

        const renderCart = () => {
            if (!cartList || !cartEmpty) return;
            const rows = [];
            state.seats.forEach((seat) => {
                rows.push({
                    type: "seat",
                    id: seat.id,
                    title: `Ve ${seat.label}`,
                    meta: state.showtime ? `${state.showtime.title} - ${state.showtime.time}` : "Ve xem phim",
                    amount: seat.price,
                });
            });
            state.food.forEach((item) => {
                rows.push({
                    type: "food",
                    id: item.id,
                    title: item.name,
                    meta: `SL ${item.quantity}`,
                    amount: Number(item.price) * Number(item.quantity),
                });
            });

            cartList.replaceChildren();
            cartEmpty.hidden = rows.length > 0;
            rows.forEach((row) => {
                const item = document.createElement("article");
                item.className = "hc-staff-pos-cart-item";
                item.innerHTML = `
                    <div>
                        <strong></strong>
                        <span></span>
                    </div>
                    <div>
                        <b></b>
                        <button type="button" aria-label="Xoa muc"><i data-lucide="x"></i></button>
                    </div>
                `;
                item.querySelector("strong").textContent = row.title;
                item.querySelector("span").textContent = row.meta;
                item.querySelector("b").textContent = currency(row.amount);
                item.querySelector("button").addEventListener("click", () => {
                    if (row.type === "seat") {
                        state.seats = state.seats.filter((seat) => String(seat.id) !== String(row.id));
                        if (state.seats.length === 0) {
                            state.showtime = null;
                            timeButtons.forEach((button) => button.classList.remove("is-selected"));
                        }
                    } else {
                        const food = root.querySelector(`[data-pos-food-id="${row.id}"]`);
                        const quantityNode = food?.querySelector("[data-pos-food-quantity]");
                        if (quantityNode) quantityNode.textContent = "0";
                        state.food.delete(String(row.id));
                    }
                    clearVoucher();
                    sync();
                });
                cartList.appendChild(item);
            });
            if (window.lucide) window.lucide.createIcons();
        };

        const sync = () => {
            const subtotal = selectedSubtotal();
            const discount = state.voucher ? Number(state.voucher.discountAmount || 0) : 0;
            const total = Math.max(0, subtotal - discount);
            if (subtotalNode) subtotalNode.textContent = currency(subtotal);
            if (discountNode) discountNode.textContent = currency(discount);
            if (totalNode) totalNode.textContent = currency(total);
            if (voucherCodeInput) voucherCodeInput.value = state.voucher?.code || "";
            if (submitButton) {
                submitButton.disabled = !(state.showtime?.id && state.seats.length > 0 && state.paymentMethod);
            }
            renderCart();
            syncHiddenInputs();
        };

        const syncModalCount = () => {
            if (modalSeatCount) modalSeatCount.textContent = String(modalState.seats.length);
            if (modalConfirm) modalConfirm.disabled = modalState.seats.length === 0 || modalState.loading;
        };

        const setTab = (name) => {
            tabButtons.forEach((button) => {
                const active = button.dataset.posTab === name;
                button.classList.toggle("is-active", active);
                button.setAttribute("aria-selected", String(active));
            });
            panes.forEach((pane) => {
                const active = pane.dataset.posPane === name;
                pane.classList.toggle("is-active", active);
                pane.hidden = !active;
            });
        };

        const setModalEmpty = (title, message) => {
            if (!seatEmpty) return;
            seatEmpty.hidden = false;
            seatEmpty.querySelector("strong").textContent = title;
            seatEmpty.querySelector("span").textContent = message;
        };

        const renderSeats = (seats) => {
            if (!seatGrid) return;
            seatGrid.replaceChildren();
            const rows = Array.from(new Set(seats.map((seat) => seat.row).filter(Boolean)));
            const maxColumn = Math.max(0, ...seats.map((seat) => Number(seat.number || 0)));
            if (rows.length > 0 && maxColumn > 0) {
                seatGrid.style.setProperty("--pos-seat-columns", String(maxColumn));
                seatGrid.style.setProperty("--pos-seat-rows", String(rows.length));
            }

            rows.forEach((row) => {
                const label = document.createElement("span");
                label.className = "hc-staff-pos-seat-row-label";
                label.textContent = row;
                label.style.gridRow = String(rowIndexFor(row, rows));
                label.style.gridColumn = "1";
                seatGrid.appendChild(label);
            });

            seats.forEach((seat) => {
                const button = document.createElement("button");
                button.type = "button";
                button.className = "hc-seat hc-staff-pos-seat";
                button.dataset.seatId = seat.seatId;
                button.dataset.seatLabel = seat.label || "";
                button.dataset.seatPrice = seat.price || 0;
                button.dataset.seatType = seat.type || "";
                button.textContent = seat.number || seat.label || "";
                button.title = `${seat.label || ""} - ${seat.displayPrice || currency(seat.price)}`;
                if (rows.length > 0 && seat.row && seat.number) {
                    button.style.gridRow = String(rowIndexFor(seat.row, rows));
                    button.style.gridColumn = String(Number(seat.number) + 1);
                }
                if (!seat.selectable) {
                    button.disabled = true;
                    button.classList.add("is-reserved");
                }
                if (modalState.seats.some((selected) => String(selected.id) === String(seat.seatId))) {
                    button.classList.add("is-selected");
                }
                button.addEventListener("click", () => {
                    if (button.disabled) return;
                    const selected = button.classList.toggle("is-selected");
                    if (selected) {
                        modalState.seats.push({
                            id: seat.seatId,
                            label: seat.label,
                            price: seat.price,
                        });
                    } else {
                        modalState.seats = modalState.seats.filter((selectedSeat) => String(selectedSeat.id) !== String(seat.seatId));
                    }
                    syncModalCount();
                });
                seatGrid.appendChild(button);
            });

            if (seatEmpty) seatEmpty.hidden = seats.length > 0;
            if (seats.length === 0) {
                setModalEmpty("Khong co ghe", "Suat chieu nay chua co ghe kha dung.");
            }
            syncModalCount();
            if (window.lucide) window.lucide.createIcons();
        };

        const closeSeatModal = () => {
            if (!modal) return;
            modal.hidden = true;
            document.body.classList.remove("hc-modal-open");
            modalState.showtime = null;
            modalState.seats = [];
            modalState.loading = false;
            syncModalCount();
        };

        const openSeatModal = async (button) => {
            if (!modal) return;
            const showtimeId = button.dataset.posShowtimeId;
            const showtime = {
                id: showtimeId,
                title: button.dataset.posShowtimeTitle || "Phim",
                format: button.dataset.posShowtimeFormat || "2D",
                hall: button.dataset.posShowtimeHall || "",
                time: button.dataset.posShowtimeTime || "",
            };
            modalState.showtime = showtime;
            modalState.seats = sameShowtime(showtime) ? state.seats.map((seat) => ({ ...seat })) : [];
            modalState.loading = true;
            modal.hidden = false;
            document.body.classList.add("hc-modal-open");
            if (modalTitle) modalTitle.textContent = `Chon ghe nhanh - ${showtime.title}`;
            if (modalMeta) modalMeta.textContent = `Suat chieu: ${showtime.time} - ${showtime.format} - ${showtime.hall}`;
            if (seatGrid) seatGrid.replaceChildren();
            setModalEmpty("Dang tai so do ghe", "Vui long cho trong giay lat.");
            syncModalCount();

            try {
                if (!seatCache.has(showtimeId)) {
                    const response = await fetch(`/api/ui/showtimes/${encodeURIComponent(showtimeId)}/seats`, {
                        headers: { "Accept": "application/json" },
                    });
                    if (!response.ok) throw new Error("seat-load-failed");
                    seatCache.set(showtimeId, await response.json());
                }
                modalState.loading = false;
                renderSeats(seatCache.get(showtimeId));
            } catch (error) {
                modalState.loading = false;
                if (seatGrid) seatGrid.replaceChildren();
                setModalEmpty("Khong the tai ghe", "Vui long thu lai hoac chon suat chieu khac.");
                syncModalCount();
            }
        };

        const commitSeatModal = () => {
            if (!modalState.showtime || modalState.seats.length === 0) return;
            state.showtime = modalState.showtime;
            state.seats = modalState.seats.map((seat) => ({ ...seat }));
            clearVoucher();
            timeButtons.forEach((button) => {
                button.classList.toggle("is-selected", String(button.dataset.posShowtimeId) === String(state.showtime.id));
            });
            closeSeatModal();
            sync();
        };

        tabButtons.forEach((button) => {
            button.addEventListener("click", () => setTab(button.dataset.posTab));
        });

        timeButtons.forEach((button) => {
            button.addEventListener("click", () => openSeatModal(button));
        });

        root.querySelectorAll("[data-pos-modal-close]").forEach((button) => {
            button.addEventListener("click", closeSeatModal);
        });
        modalConfirm?.addEventListener("click", commitSeatModal);
        document.addEventListener("keydown", (event) => {
            if (event.key === "Escape" && modal && !modal.hidden) {
                closeSeatModal();
            }
        });

        foodItems.forEach((item) => {
            const quantityNode = item.querySelector("[data-pos-food-quantity]");
            const updateQuantity = (quantity) => {
                const safeQuantity = Math.max(0, quantity);
                if (quantityNode) quantityNode.textContent = String(safeQuantity);
                const id = String(item.dataset.posFoodId);
                if (safeQuantity > 0) {
                    state.food.set(id, {
                        id,
                        name: item.dataset.posFoodName || "",
                        price: Number(item.dataset.posFoodPrice || 0),
                        quantity: safeQuantity,
                    });
                } else {
                    state.food.delete(id);
                }
                clearVoucher();
                sync();
            };
            item.querySelector("[data-pos-food-decrease]")?.addEventListener("click", () => {
                updateQuantity(Number(quantityNode?.textContent || 0) - 1);
            });
            item.querySelector("[data-pos-food-increase]")?.addEventListener("click", () => {
                updateQuantity(Number(quantityNode?.textContent || 0) + 1);
            });
        });

        paymentButtons.forEach((button) => {
            button.addEventListener("click", () => {
                state.paymentMethod = button.dataset.posPayment || "";
                paymentButtons.forEach((item) => item.classList.toggle("is-selected", item === button));
                sync();
            });
        });

        root.querySelector("[data-pos-voucher-apply]")?.addEventListener("click", async () => {
            const code = voucherInput?.value?.trim() || "";
            const subtotal = selectedSubtotal();
            if (!code || subtotal <= 0) {
                clearVoucher();
                if (voucherMessage) voucherMessage.textContent = "Nhap voucher sau khi da chon san pham.";
                sync();
                return;
            }
            try {
                const params = new URLSearchParams({
                    code,
                    orderValue: String(subtotal),
                });
                if (root.dataset.branchId) params.set("branchId", root.dataset.branchId);
                const response = await fetch(`/api/ui/vouchers/preview?${params.toString()}`, {
                    headers: { "Accept": "application/json" },
                });
                if (!response.ok) throw new Error("voucher-preview-failed");
                const preview = await response.json();
                if (!preview.valid) {
                    clearVoucher();
                    if (voucherMessage) voucherMessage.textContent = "Voucher khong hop le hoac chua du dieu kien.";
                } else {
                    state.voucher = {
                        code: preview.code || code,
                        discountAmount: preview.discountAmount || 0,
                    };
                    if (voucherMessage) voucherMessage.textContent = `Da ap dung ${preview.displayDiscount || currency(preview.discountAmount)}.`;
                }
            } catch (error) {
                clearVoucher();
                if (voucherMessage) voucherMessage.textContent = "Khong the kiem tra voucher luc nay.";
            }
            sync();
        });

        root.querySelector("[data-pos-customer-search]")?.addEventListener("click", () => {
            const phone = customerPhone?.value?.trim() || "";
            if (customerMessage) {
                customerMessage.textContent = phone
                    ? "So dien thoai se duoc dung de gan thanh vien neu tim thay."
                    : "Nhap so dien thoai truoc khi tim.";
            }
        });

        root.querySelector("[data-pos-clear]")?.addEventListener("click", () => {
            state.showtime = null;
            state.seats = [];
            state.food.clear();
            state.paymentMethod = "";
            clearVoucher();
            closeSeatModal();
            timeButtons.forEach((button) => button.classList.remove("is-selected"));
            paymentButtons.forEach((button) => button.classList.remove("is-selected"));
            foodItems.forEach((item) => {
                const quantityNode = item.querySelector("[data-pos-food-quantity]");
                if (quantityNode) quantityNode.textContent = "0";
            });
            if (voucherInput) voucherInput.value = "";
            if (customerMessage) customerMessage.textContent = "";
            sync();
        });

        form?.addEventListener("submit", (event) => {
            sync();
            if (submitButton?.disabled) {
                event.preventDefault();
            }
        });

        sync();
    });
})();
