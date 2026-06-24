(function () {
  const layoutForm = document.querySelector('[data-hall-layout-form]');
  if (!layoutForm) {
    return;
  }

  const rowInput = layoutForm.querySelector('[data-hall-row-count]');
  const columnInput = layoutForm.querySelector('[data-hall-column-count]');
  const capacityInput = layoutForm.querySelector('[data-hall-capacity]');

  const updateCapacity = () => {
    const rows = Number.parseInt(rowInput?.value || '', 10);
    const columns = Number.parseInt(columnInput?.value || '', 10);
    capacityInput.value = Number.isInteger(rows) && Number.isInteger(columns) && rows > 0 && columns > 0
      ? String(rows * columns)
      : '';
  };

  rowInput?.addEventListener('input', updateCapacity);
  columnInput?.addEventListener('input', updateCapacity);
  updateCapacity();
})();
