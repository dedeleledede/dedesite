document.addEventListener('DOMContentLoaded', () => {
    const formatTime = (value) => {
        const digits = value.replace(/\D/g, '').slice(0, 4);
        return digits.length > 2 ? `${digits.slice(0, 2)}:${digits.slice(2)}` : digits;
    };

    document.querySelectorAll('.observatory-time-entry').forEach((input) => {
        input.addEventListener('input', () => {
            input.value = formatTime(input.value);
            input.dispatchEvent(new Event('observatory:time-change', { bubbles: true }));
        });
    });

    document.querySelectorAll('.observatory-datetime-parts').forEach((group) => {
        const hidden = group.querySelector('input[type="hidden"]');
        const date = group.querySelector('.observatory-date-entry');
        const time = group.querySelector('.observatory-time-entry');
        const sync = () => {
            hidden.value = date.value && /^\d{2}:\d{2}$/.test(time.value)
                ? `${date.value}T${time.value}`
                : '';
        };
        date.addEventListener('input', sync);
        time.addEventListener('observatory:time-change', sync);
    });
});
