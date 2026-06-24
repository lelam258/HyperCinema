(() => {
const initHyperCinemaApp = () => {
    if (window.lucide) {
        window.lucide.createIcons();
    }

    document.querySelectorAll("[data-booking-seat-grid]").forEach((grid) => {
        if (grid.dataset.bookingSeatReady === "true") return;
        grid.dataset.bookingSeatReady = "true";

        const root = grid.closest("[data-booking-root]") || grid.closest(".hc-grid-two") || document;
        const countNode = root.querySelector("[data-booking-seat-count]");
        const summaryNodes = root.querySelectorAll("[data-booking-seat-summary]");
        const totalNode = root.querySelector("[data-booking-total]");
        const submitButton = root.querySelector("[data-booking-submit]");
        const selectedSeatsNode = root.querySelector("[data-booking-selected-seats]");
        const selectedFoodNode = root.querySelector("[data-booking-selected-food]");
        const foodItems = Array.from(root.querySelectorAll("[data-food-id]"));

        const syncSeatGridLayout = () => {
            if (!grid.closest(".hc-customer-booking-shell")) return;
            grid.querySelectorAll(".hc-booking-aisle-marker").forEach((marker) => marker.remove());
            const seats = Array.from(grid.querySelectorAll(".hc-seat"));
            const rows = [];
            const occupiedColumns = new Set();
            let maxColumn = 0;

            seats.forEach((seat) => {
                const row = seat.dataset.seatRow;
                const number = Number(seat.dataset.seatNumber || 0);
                if (!row || !number) return;
                if (!rows.includes(row)) rows.push(row);
                occupiedColumns.add(number);
                maxColumn = Math.max(maxColumn, number);
            });

            if (rows.length === 0 || maxColumn === 0) return;

            const singleLetterRows = rows.every((row) => /^[A-Za-z]$/.test(row));
            const rowBase = singleLetterRows
                ? Math.min(...rows.map((row) => row.toUpperCase().charCodeAt(0)))
                : 0;
            const rowIndexFor = (row) => singleLetterRows
                ? row.toUpperCase().charCodeAt(0) - rowBase + 1
                : rows.indexOf(row) + 1;
            const occupiedRows = new Set(rows.map(rowIndexFor));
            const maxRow = Math.max(...occupiedRows);

            grid.style.setProperty("--booking-seat-columns", String(maxColumn));
            grid.style.setProperty("--booking-seat-rows", String(maxRow));
            seats.forEach((seat) => {
                const rowIndex = rowIndexFor(seat.dataset.seatRow || "");
                const columnIndex = Number(seat.dataset.seatNumber || 0);
                if (rowIndex > 0 && columnIndex > 0) {
                    seat.style.gridRow = String(rowIndex);
                    seat.style.gridColumn = String(columnIndex);
                }
            });

            for (let column = 1; column <= maxColumn; column++) {
                if (occupiedColumns.has(column)) continue;
                const marker = document.createElement("span");
                marker.className = "hc-booking-aisle-marker is-column";
                marker.setAttribute("aria-hidden", "true");
                marker.style.gridColumn = String(column);
                marker.style.gridRow = `1 / ${maxRow + 1}`;
                grid.appendChild(marker);
            }

            for (let row = 1; row <= maxRow; row++) {
                if (occupiedRows.has(row)) continue;
                const marker = document.createElement("span");
                marker.className = "hc-booking-aisle-marker is-row";
                marker.setAttribute("aria-hidden", "true");
                marker.style.gridColumn = `1 / ${maxColumn + 1}`;
                marker.style.gridRow = String(row);
                grid.appendChild(marker);
            }
        };

        const formatCurrency = (value) =>
            new Intl.NumberFormat("vi-VN").format(value || 0) + " VND";

        const syncSelectedSeatInputs = (selectedSeats) => {
            if (!selectedSeatsNode) return;
            selectedSeatsNode.replaceChildren();
            selectedSeats.forEach((seat) => {
                const seatId = seat.dataset.seatId;
                if (!seatId) return;
                const input = document.createElement("input");
                input.type = "hidden";
                input.name = "seatIds";
                input.value = seatId;
                selectedSeatsNode.appendChild(input);
            });
        };

        const selectedFood = () => foodItems
            .map((item) => ({
                item,
                id: item.dataset.foodId,
                name: item.dataset.foodName || "",
                price: Number(item.dataset.foodPrice || 0),
                quantity: Number(item.querySelector("[data-food-quantity]")?.textContent || 0),
            }))
            .filter((food) => food.id && food.quantity > 0);

        const syncSelectedFoodInputs = (foods) => {
            if (!selectedFoodNode) return;
            selectedFoodNode.replaceChildren();
            foods.forEach((food) => {
                const itemIdInput = document.createElement("input");
                itemIdInput.type = "hidden";
                itemIdInput.name = "foodItemIds";
                itemIdInput.value = food.id;

                const quantityInput = document.createElement("input");
                quantityInput.type = "hidden";
                quantityInput.name = "foodQuantities";
                quantityInput.value = String(food.quantity);

                selectedFoodNode.append(itemIdInput, quantityInput);
            });
        };

        const updateSummary = () => {
            const selectedSeats = Array.from(grid.querySelectorAll(".hc-seat.is-selected"));
            const foods = selectedFood();
            const labels = selectedSeats.map((seat) => seat.dataset.seatLabel || seat.textContent.trim());
            const seatTotal = selectedSeats.reduce((sum, seat) => sum + Number(seat.dataset.seatPrice || 0), 0);
            const foodTotal = foods.reduce((sum, food) => sum + food.price * food.quantity, 0);

            syncSelectedSeatInputs(selectedSeats);
            syncSelectedFoodInputs(foods);
            if (countNode) countNode.textContent = String(selectedSeats.length);
            summaryNodes.forEach((summaryNode) => {
                summaryNode.textContent = labels.length > 0 ? labels.join(", ") : "Chưa chọn ghế nào";
            });
            if (totalNode) totalNode.textContent = formatCurrency(seatTotal + foodTotal);
            if (submitButton) submitButton.disabled = selectedSeats.length === 0;
        };

        grid.addEventListener("click", (event) => {
            const seat = event.target.closest(".hc-seat");
            if (!seat || seat.disabled) return;
            event.preventDefault();
            seat.classList.toggle("is-selected");
            updateSummary();
        });

        foodItems.forEach((item) => {
            const quantityNode = item.querySelector("[data-food-quantity]");
            item.querySelector("[data-food-decrease]")?.addEventListener("click", () => {
                const quantity = Math.max(0, Number(quantityNode?.textContent || 0) - 1);
                if (quantityNode) quantityNode.textContent = String(quantity);
                updateSummary();
            });
            item.querySelector("[data-food-increase]")?.addEventListener("click", () => {
                const quantity = Number(quantityNode?.textContent || 0) + 1;
                if (quantityNode) quantityNode.textContent = String(quantity);
                updateSummary();
            });
        });

        syncSeatGridLayout();
        updateSummary();
    });
};

if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", initHyperCinemaApp);
} else {
    initHyperCinemaApp();
}
})();
