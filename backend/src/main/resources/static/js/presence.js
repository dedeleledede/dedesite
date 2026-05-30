(() => {
    const ping = () => fetch("/presence/ping", {
        method: "POST",
        credentials: "same-origin",
        keepalive: true
    }).catch(() => {});

    ping();
    window.setInterval(ping, 60_000);
})();
