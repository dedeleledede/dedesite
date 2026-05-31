document.addEventListener('DOMContentLoaded', () => {
    const formatTime = (value) => {
        const digits = value.replace(/\D/g, '').slice(0, 4);
        return digits.length > 2 ? `${digits.slice(0, 2)}:${digits.slice(2)}` : digits;
    };

    const syncTimeSelects = (input, hourSelect, minuteSelect) => {
        const match = /^([0-2]\d):([0-5]\d)$/.exec(input.value);
        if (match) {
            hourSelect.value = match[1];
            minuteSelect.value = match[2];
        }
    };

    const formatVisibleDate = (value) => {
        const digits = value.replace(/\D/g, '').slice(0, 8);
        if (digits.length > 4) {
            return `${digits.slice(0, 2)}/${digits.slice(2, 4)}/${digits.slice(4)}`;
        }
        if (digits.length > 2) {
            return `${digits.slice(0, 2)}/${digits.slice(2)}`;
        }
        return digits;
    };

    const isoToVisibleDate = (value) => {
        const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
        return match ? `${match[3]}/${match[2]}/${match[1]}` : '';
    };

    const visibleToIsoDate = (value) => {
        const match = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(value);
        if (!match) {
            return '';
        }
        const iso = `${match[3]}-${match[2]}-${match[1]}`;
        const parsed = new Date(`${iso}T00:00:00`);
        return parsed.getFullYear() === Number(match[3])
            && parsed.getMonth() + 1 === Number(match[2])
            && parsed.getDate() === Number(match[1])
            ? iso
            : '';
    };

    document.querySelectorAll('.observatory-time-entry').forEach((input) => {
        const control = document.createElement('span');
        const picker = document.createElement('span');
        control.className = 'observatory-time-control';
        picker.className = 'observatory-time-picker';
        const hourSelect = document.createElement('select');
        const minuteSelect = document.createElement('select');
        hourSelect.setAttribute('aria-label', 'Hour');
        minuteSelect.setAttribute('aria-label', 'Minute');

        for (let hour = 0; hour < 24; hour += 1) {
            hourSelect.add(new Option(String(hour).padStart(2, '0'), String(hour).padStart(2, '0')));
        }
        for (let minute = 0; minute < 60; minute += 1) {
            minuteSelect.add(new Option(String(minute).padStart(2, '0'), String(minute).padStart(2, '0')));
        }

        const separator = document.createElement('span');
        separator.textContent = ':';
        picker.append(hourSelect, separator, minuteSelect);
        input.replaceWith(control);
        control.append(input, picker);
        syncTimeSelects(input, hourSelect, minuteSelect);

        const syncInput = () => {
            input.value = `${hourSelect.value}:${minuteSelect.value}`;
            input.dispatchEvent(new Event('input', { bubbles: true }));
        };
        hourSelect.addEventListener('change', syncInput);
        minuteSelect.addEventListener('change', syncInput);
        input.addEventListener('input', () => {
            input.value = formatTime(input.value);
            syncTimeSelects(input, hourSelect, minuteSelect);
            input.dispatchEvent(new Event('observatory:time-change', { bubbles: true }));
        });
    });

    document.querySelectorAll('.observatory-page input[type="date"]').forEach((nativeInput) => {
        const control = document.createElement('span');
        const visibleInput = document.createElement('input');
        const trigger = document.createElement('button');
        const glyph = document.createElement('span');
        control.className = 'observatory-date-control';
        visibleInput.type = 'text';
        visibleInput.className = 'observatory-date-display';
        visibleInput.inputMode = 'numeric';
        visibleInput.placeholder = 'dd/mm/aaaa';
        visibleInput.pattern = '[0-9]{2}/[0-9]{2}/[0-9]{4}';
        visibleInput.value = isoToVisibleDate(nativeInput.value);
        trigger.type = 'button';
        trigger.className = 'observatory-calendar-trigger';
        trigger.setAttribute('aria-label', 'Open calendar');
        glyph.className = 'observatory-calendar-glyph';
        glyph.setAttribute('aria-hidden', 'true');
        trigger.append(glyph);
        nativeInput.classList.add('observatory-native-date');
        nativeInput.replaceWith(control);
        control.append(visibleInput, trigger, nativeInput);
        let editingVisibleInput = false;

        visibleInput.addEventListener('input', () => {
            visibleInput.value = formatVisibleDate(visibleInput.value);
            editingVisibleInput = true;
            nativeInput.value = visibleToIsoDate(visibleInput.value);
            nativeInput.dispatchEvent(new Event('input', { bubbles: true }));
            editingVisibleInput = false;
        });
        nativeInput.addEventListener('input', () => {
            if (!editingVisibleInput) {
                visibleInput.value = isoToVisibleDate(nativeInput.value);
            }
        });
        trigger.addEventListener('click', () => {
            try {
                if (typeof nativeInput.showPicker === 'function') {
                    nativeInput.showPicker();
                } else {
                    nativeInput.click();
                }
            } catch {
                nativeInput.click();
            }
        });
    });

    document.querySelectorAll('.observatory-datetime-parts').forEach((group) => {
        const hidden = group.querySelector('input[type="hidden"]');
        const date = group.querySelector('.observatory-date-entry');
        const time = group.querySelector('.observatory-time-entry, input[type="time"]');
        const sync = () => {
            hidden.value = date.value && /^\d{2}:\d{2}$/.test(time.value)
                ? `${date.value}T${time.value}`
                : '';
        };
        date.addEventListener('input', sync);
        time.addEventListener('input', sync);
        time.addEventListener('change', sync);
    });
});
