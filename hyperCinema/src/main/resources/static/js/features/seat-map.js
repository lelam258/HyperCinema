(function () {
    document.addEventListener('click', (event) => {
        const seat = event.target.closest('[data-seat]');
        if (!seat || seat.disabled || seat.classList.contains('is-reserved')) {
            return;
        }

        seat.classList.toggle('is-selected');
        const selectedSeats = Array.from(document.querySelectorAll('[data-seat].is-selected'))
            .map((node) => node.dataset.seat)
            .filter(Boolean);

        document.querySelectorAll('[data-seat-output]').forEach((output) => {
            output.value = selectedSeats.join(',');
            output.dispatchEvent(new Event('change', { bubbles: true }));
        });

        document.querySelectorAll('[data-seat-count]').forEach((node) => {
            node.textContent = String(selectedSeats.length);
        });
    });
})();
