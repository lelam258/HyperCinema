(function () {
  document.querySelectorAll('[data-showtime-form]').forEach((form) => {
    const movieSelect = form.querySelector('[data-showtime-movie]');
    const branchSelect = form.querySelector('[data-showtime-branch]');
    const hallSelect = form.querySelector('[data-showtime-hall]');
    const startInput = form.querySelector('[data-showtime-start]');
    const endInput = form.querySelector('[data-showtime-end]');

    if (!hallSelect) {
      return;
    }

    const syncHallOptions = () => {
      const selectedBranchId = branchSelect?.value || '';
      const currentHall = hallSelect.selectedOptions[0];
      let currentHallStillVisible = !currentHall || currentHall.value === '';

      Array.from(hallSelect.options).forEach((option) => {
        if (option.value === '') {
          option.hidden = false;
          option.disabled = false;
          return;
        }

        const matchesBranch = !selectedBranchId || option.dataset.branchId === selectedBranchId;
        option.hidden = !matchesBranch;
        option.disabled = !matchesBranch;

        if (option.selected && matchesBranch) {
          currentHallStillVisible = true;
        }
      });

      if (!currentHallStillVisible) {
        hallSelect.value = '';
      }
    };

    branchSelect?.addEventListener('change', syncHallOptions);
    syncHallOptions();

    if (!movieSelect || !startInput || !endInput) {
      return;
    }

    const formatDateTimeLocal = (date) => {
      const pad = (value) => String(value).padStart(2, '0');
      return [
        date.getFullYear(),
        pad(date.getMonth() + 1),
        pad(date.getDate())
      ].join('-') + 'T' + [
        pad(date.getHours()),
        pad(date.getMinutes())
      ].join(':');
    };

    const syncEndTime = () => {
      const selectedMovie = movieSelect.selectedOptions[0];
      const durationMinutes = Number.parseInt(selectedMovie?.dataset.durationMinutes || '', 10);

      if (!startInput.value || !Number.isInteger(durationMinutes) || durationMinutes <= 0) {
        return;
      }

      if (endInput.value && endInput.value !== lastAutoEnd) {
        return;
      }

      const startTime = new Date(startInput.value);
      if (Number.isNaN(startTime.getTime())) {
        return;
      }

      startTime.setMinutes(startTime.getMinutes() + durationMinutes);
      endInput.value = formatDateTimeLocal(startTime);
    };

    movieSelect.addEventListener('change', syncEndTime);
    startInput.addEventListener('input', syncEndTime);
    syncEndTime();
  });
})();
