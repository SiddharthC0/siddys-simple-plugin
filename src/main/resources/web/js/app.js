(() => {
    const $ = selector => document.querySelector(selector);
    const $$ = selector => [...document.querySelectorAll(selector)];

    let currentFile = "";
    let fileList = [];
    let consoleStream = null;
    let consoleLineCount = 0;
    let resourceTimer = null;
    let monitorTimer = null;

    function startMonitorUpdates() {
        refreshMonitor();

        if (monitorTimer !== null) {
            clearInterval(monitorTimer);
        }

        monitorTimer = setInterval(refreshMonitor, 2000);
    }

    function toast(message) {
        const element = $("#toast");
        if (!element) return;

        element.textContent = message;
        element.classList.add("show");

        clearTimeout(toast.timer);
        toast.timer = setTimeout(() => element.classList.remove("show"), 2600);
    }

    function titleCase(value) {
        return value.split(/[-_.]/).filter(Boolean).map(part => part[0].toUpperCase() + part.slice(1)).join(" ");
    }

    function setView(view, title) {
        $$(".view").forEach(element => {
            element.classList.toggle("hidden", element.id !== `view-${view}`);
        });

        $$(".nav-item[data-view]").forEach(element => {
            element.classList.toggle("active", element.dataset.view === view);
        });

        const pageTitle = $("#page-title");
        if (pageTitle) pageTitle.textContent = title || titleCase(view);
        if (view === "monitor") requestAnimationFrame(renderMonitorGraphs);
    }

    function buildNavigation(files) {
        const nav = $("#file-nav");
        nav.innerHTML = "";

        const folders = new Map();

        files.forEach(path => {
            const parts = path.split("/");
            const folder = parts.length > 1 ? parts.slice(0, -1).join("/") : "Root";

            if (!folders.has(folder)) folders.set(folder, []);
            folders.get(folder).push(path);
        });

        [...folders.keys()].sort().forEach(folder => {
            const group = document.createElement("div");
            group.className = "nav-group";

            const label = document.createElement("div");
            label.className = "nav-group-title";
            label.textContent = folder;
            group.append(label);

            folders.get(folder).forEach(path => {
                const button = document.createElement("button");
                button.className = "nav-item";
                button.title = path;
                button.innerHTML = "<span>▤</span>";

                const text = document.createElement("span");
                text.textContent = path.split("/").at(-1);
                text.style.cssText = "width:auto;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;text-align:left;font-size:12px";

                button.append(text);
                button.addEventListener("click", () => loadFile(path));
                group.append(button);
            });

            nav.append(group);
        });

    }

    async function refreshFiles() {
        const result = await SSMPApi.files();
        fileList = result.files || [];

        buildNavigation(fileList);
    }

    const monitorHistory = {
        cpu: [],
        ram: [],
        tps: [],
        mspt: []
    };

    const monitorHistoryLimit = 60;

    function addMonitorSample(key, value) {
        if (!Number.isFinite(value) || value < 0) return;

        const history = monitorHistory[key];
        history.push(value);

        if (history.length > monitorHistoryLimit) {
            history.shift();
        }
    }

    function drawMonitorGraph(canvasId, historyKey, maximum) {
        const canvas = document.getElementById(canvasId);
        if (!canvas) return;

        const context = canvas.getContext("2d");
        const rect = canvas.getBoundingClientRect();

        if (!rect.width || !rect.height) return;

        const ratio = window.devicePixelRatio || 1;
        const width = rect.width;
        const height = rect.height;

        canvas.width = Math.round(width * ratio);
        canvas.height = Math.round(height * ratio);

        context.setTransform(ratio, 0, 0, ratio, 0, 0);
        context.clearRect(0, 0, width, height);

        const padding = {
            top: 12,
            right: 8,
            bottom: 12,
            left: 8
        };

        const graphWidth = width - padding.left - padding.right;
        const graphHeight = height - padding.top - padding.bottom;
        const values = monitorHistory[historyKey];

        context.strokeStyle = "#292929";
        context.lineWidth = 1;

        for (let i = 0; i <= 4; i++) {
            const y = padding.top + graphHeight * (i / 4);

            context.beginPath();
            context.moveTo(padding.left, y);
            context.lineTo(width - padding.right, y);
            context.stroke();
        }

        if (values.length < 2) return;

        const scaleMaximum = maximum || Math.max(
            10,
            Math.ceil(Math.max(...values) / 10) * 10
        );

        const points = values.map((value, index) => ({
            x: padding.left + graphWidth * (
                index / Math.max(1, values.length - 1)
            ),
            y: padding.top + graphHeight * (
                1 - Math.min(value, scaleMaximum) / scaleMaximum
            )
        }));

        context.beginPath();
        context.moveTo(points[0].x, points[0].y);

        for (let i = 1; i < points.length; i++) {
            context.lineTo(points[i].x, points[i].y);
        }

        context.strokeStyle = "#b7ff35";
        context.lineWidth = 2;
        context.lineJoin = "round";
        context.lineCap = "round";
        context.stroke();

        const last = points[points.length - 1];

        context.lineTo(last.x, height - padding.bottom);
        context.lineTo(points[0].x, height - padding.bottom);
        context.closePath();

        const gradient = context.createLinearGradient(
            0,
            padding.top,
            0,
            height - padding.bottom
        );

        gradient.addColorStop(0, "rgba(183, 255, 53, 0.20)");
        gradient.addColorStop(1, "rgba(183, 255, 53, 0)");

        context.fillStyle = gradient;
        context.fill();

        context.beginPath();
        context.arc(last.x, last.y, 3.5, 0, Math.PI * 2);
        context.fillStyle = "#b7ff35";
        context.fill();
    }

    function renderMonitorGraphs() {
        drawMonitorGraph("graph-cpu", "cpu", 100);
        drawMonitorGraph("graph-ram", "ram", 100);
        drawMonitorGraph("graph-tps", "tps", 20);
        drawMonitorGraph("graph-mspt", "mspt", 100);
    }

    function updateMonitorGraphLabels(data) {
        const labels = {
            "graph-cpu-value": data.cpuPercent >= 0
                ? `${data.cpuPercent.toFixed(1)}%`
                : "N/A",
            "graph-ram-value": `${data.ramPercent.toFixed(1)}%`,
            "graph-tps-value": data.tps.toFixed(1),
            "graph-mspt-value": `${data.mspt.toFixed(1)} ms`
        };

        for (const [id, value] of Object.entries(labels)) {
            const element = document.getElementById(id);
            if (element) element.textContent = value;
        }
    }

    function toFiniteNumber(value, fallback = -1) {
        const number = Number(value);
        return Number.isFinite(number) ? number : fallback;
    }

    function normalizeMonitorData(raw) {
        const data = { ...raw };

        let used = toFiniteNumber(
            data.ramUsedBytes ?? data.usedMemoryBytes ?? data.ramUsed ?? data.usedMemory,
            0
        );

        let max = toFiniteNumber(
            data.ramMaxBytes ?? data.maxMemoryBytes ?? data.ramMax ?? data.maxMemory,
            0
        );

        if (used > 10000000) used /= 1048576;
        if (max > 10000000) max /= 1048576;

        data.ramUsed = used;
        data.ramMax = max;
        data.cpuPercent = toFiniteNumber(data.cpuPercent);
        data.ramPercent = toFiniteNumber(
            data.ramPercent,
            max > 0 ? used / max * 100 : -1
        );
        data.tps = toFiniteNumber(data.tps);
        data.mspt = toFiniteNumber(data.mspt);

        return data;
    }

    async function refreshMonitor() {
        try {
            const data = normalizeMonitorData(await SSMPApi.monitor());

            addMonitorSample("cpu", data.cpuPercent);
            addMonitorSample("ram", data.ramPercent);
            addMonitorSample("tps", data.tps);
            addMonitorSample("mspt", data.mspt);

            const cpu = $("#monitor-cpu");
            const ram = $("#monitor-ram");
            const tps = $("#monitor-tps");
            const mspt = $("#monitor-mspt");
            const ramDetails = $("#monitor-ram-details");

            if (cpu) cpu.textContent = data.cpuPercent >= 0
                ? `${data.cpuPercent.toFixed(1)}%`
                : "N/A";

            if (ram) ram.textContent = data.ramPercent >= 0
                ? `${data.ramPercent.toFixed(1)}%`
                : "N/A";

            if (tps) tps.textContent = data.tps >= 0
                ? data.tps.toFixed(1)
                : "N/A";

            if (mspt) mspt.textContent = data.mspt >= 0
                ? `${data.mspt.toFixed(1)} ms`
                : "N/A";

            if (ramDetails) ramDetails.textContent = data.ramMax > 0
                ? `${data.ramUsed.toFixed(0)} MB / ${data.ramMax.toFixed(0)} MB`
                : "N/A";

            updateMonitorGraphLabels(data);
            renderMonitorGraphs();
        } catch (error) {
            console.error("Monitor update failed:", error);
        }
    }

        function renderControl(entry) {
        const wrap = document.createElement("div");
        wrap.className = "setting-control";

        const state = document.createElement("span");
        state.className = "save-state";
        state.textContent = "";

        let control;

        if (entry.type === "boolean") {
            control = document.createElement("input");
            control.type = "checkbox";
            control.className = "toggle";
            control.checked = entry.value === "true";

            control.addEventListener("change", () => {
                save(entry, control.checked ? "true" : "false", "boolean", state);
            });
        } else if (entry.type === "number") {
            control = document.createElement("input");
            control.type = "number";
            control.className = "field";
            control.value = entry.value;

            control.addEventListener("change", () => {
                save(entry, control.value, "number", state);
            });
        } else if (entry.type === "list") {
            control = document.createElement("textarea");
            control.className = "field";
            control.rows = Math.min(5, Math.max(2, entry.value.split("\n").length));
            control.value = entry.value;

            control.addEventListener("change", () => {
                save(entry, control.value, "list", state);
            });
        } else {
            control = document.createElement("input");
            control.type = "text";
            control.className = "field";
            control.value = entry.value;

            control.addEventListener("change", () => {
                save(entry, control.value, "string", state);
            });
        }

        wrap.append(control, state);
        return wrap;
    }

    async function save(entry, value, type, state) {
        state.textContent = "Saving…";

        try {
            await SSMPApi.save(currentFile, entry.key, value, type);
            state.textContent = "Saved";
            toast("Configuration saved");

            if (entry.key.startsWith("modules.") && entry.key.endsWith(".enabled")) {
                toast("Saved. Restart required for module changes.");
            }
        } catch (exception) {
            state.textContent = "Failed";
            toast(exception.message);
        }
    }

    async function loadFile(path) {
        try {
            const data = await SSMPApi.file(path);
            currentFile = path;

            $("#file-title").textContent = path.split("/").at(-1);
            $("#file-path").textContent = path;

            const list = $("#settings-list");
            list.innerHTML = "";

            data.entries.forEach(entry => {
                if (entry.section) {
                    const heading = document.createElement("div");
                    heading.className = "setting-section";
                    heading.textContent = titleCase(entry.key.split(".").at(-1));
                    list.append(heading);
                    return;
                }

                const row = document.createElement("div");
                row.className = "setting-row";

                const info = document.createElement("div");
                info.className = "setting-info";

                const name = document.createElement("div");
                name.className = "setting-name";
                name.textContent = titleCase(entry.key.split(".").at(-1));

                const key = document.createElement("div");
                key.className = "setting-key";
                key.textContent = entry.key;

                info.append(name, key);
                row.append(info, renderControl(entry));
                list.append(row);
            });

            $("#empty-file").classList.toggle("hidden", data.entries.length > 0);

            setView("file", path.split("/").at(-1));

            $$(".file-nav .nav-item").forEach(button => {
                button.classList.toggle("active", button.title === path);
            });
        } catch (exception) {
            toast(exception.message);
        }
    }

    async function renderModules() {
        const config = fileList.find(path =>
            path.toLowerCase() === "config.yml" ||
            path.toLowerCase() === "config.yaml"
        );

        const list = $("#module-list");
        list.innerHTML = "";

        if (!config) {
            list.textContent = "No root config.yml was found.";
            return;
        }

        const data = await SSMPApi.file(config);

        const modules = data.entries.filter(entry =>
            entry.key.startsWith("modules.") &&
            entry.key.endsWith(".enabled")
        );

        modules.forEach(entry => {
            const row = document.createElement("div");
            row.className = "module-row";

            const info = document.createElement("div");

            const name = document.createElement("b");
            name.textContent = titleCase(entry.key.split(".")[1] || entry.key);

            const desc = document.createElement("small");
            desc.textContent = entry.key + " · restart may be required";

            info.append(name, desc);

            const toggle = document.createElement("input");
            toggle.type = "checkbox";
            toggle.className = "toggle";
            toggle.checked = entry.value === "true";

            toggle.addEventListener("change", async () => {
                try {
                    await SSMPApi.save(
                        config,
                        entry.key,
                        toggle.checked ? "true" : "false",
                        "boolean"
                    );

                    toast("Module setting saved. Restart required.");
                } catch (error) {
                    toggle.checked = !toggle.checked;
                    toast(error.message);
                }
            });

            row.append(info, toggle);
            list.append(row);
        });
    }

    function setConsoleStatus(status, connected = false) {
        const label = $("#console-status");
        const indicator = $("#console-status-indicator");

        if (!label || !indicator) return;

        label.textContent = status;
        indicator.classList.toggle("connected", connected);
        indicator.classList.toggle("disconnected", !connected);
    }

    const MAX_CONSOLE_LINES = 2000;

function appendConsoleLine(message, type = "normal") {
    appendConsoleLines([message], type);
}

function appendConsoleLines(messages, type = "normal") {
    const output = $("#console-output");
    if (!output || !messages.length) return;

    const placeholder = output.querySelector(".console-placeholder");
    if (placeholder) placeholder.remove();

    const fragment = document.createDocumentFragment();

    messages.forEach(message => {
        const line = document.createElement("div");
        line.className = "console-line console-line-" + type;
        line.textContent = String(message);
        fragment.append(line);
    });

    output.append(fragment);

    consoleLineCount = output.querySelectorAll(".console-line").length;

    while (consoleLineCount > MAX_CONSOLE_LINES) {
        const excess = consoleLineCount - MAX_CONSOLE_LINES;

        for (let i = 0; i < excess; i++) {
            output.querySelector(".console-line")?.remove();
        }

        consoleLineCount = MAX_CONSOLE_LINES;
    }

    output.scrollTop = output.scrollHeight;
}

    let consolePollCursor = 0;
let consolePollTimer = null;

async function connectConsole() {
    setConsoleStatus("Checking…");

    try {
        const status = await SSMPApi.consoleStatus();
        if (status.enabled !== true) {
            setConsoleStatus("Disabled");
            appendConsoleLine("Console access is disabled in config.yml.", "error");
            return;
        }

        setConsoleStatus("Connected", true);
        consolePollCursor = 0;
        pollConsole();
    } catch (error) {
        setConsoleStatus("Unavailable");
        appendConsoleLine("Console initialization failed: " + error.message, "error");
    }
}

async function pollConsole() {
    if (consolePollTimer !== null) {
        clearTimeout(consolePollTimer);
        consolePollTimer = null;
    }

    try {
        const data = await SSMPApi.request(
            "/api/console/poll?since=" + consolePollCursor
        );

        if (Array.isArray(data.lines) && data.lines.length > 0) {
            appendConsoleLines(data.lines);
        }

        if (typeof data.next === "number") {
            consolePollCursor = data.next;
        }

        setConsoleStatus("Connected", true);

    } catch (error) {
        setConsoleStatus("Connection lost");
    }

    consolePollTimer = setTimeout(pollConsole, 1000);
}
    async function sendConsoleCommand(command) {
    const input = $("#console-command");
    const send = $("#console-send");
    const trimmed = command.trim();

    if (!trimmed) return;

    send.disabled = true;

    try {
        await SSMPApi.consoleCommand(trimmed);
        // Don't echo here — the SSE stream delivers the real output.
        // A premature echo before the output arrives is what makes it
        // look like the command produced no result.
        input.value = "";
    } catch (error) {
        appendConsoleLine("Command failed: " + error.message, "error");
        toast(error.message);
    } finally {
        send.disabled = false;
        input.focus();
    }
}
        async function refreshResources() {
        try {
            const data = await SSMPApi.resources();

            const ram = Number(data.ramPercent);
            const cpu = Number(data.cpuPercent);
            const average = Number(data.averagePercent);

            $("#stat-resources").textContent =
                Number.isFinite(average) && average >= 0
                    ? Math.round(average) + "%"
                    : "—";

            const ramText = Number.isFinite(ram) && ram >= 0
                ? "RAM " + Math.round(ram) + "%"
                : "RAM unavailable";

            const cpuText = Number.isFinite(cpu) && cpu >= 0
                ? "CPU " + Math.round(cpu) + "%"
                : "CPU unavailable";

            $("#stat-resource-details").textContent = ramText + " · " + cpuText;
        } catch {
            $("#stat-resource-details").textContent = "Statistics unavailable";
        }
    }

    function renderServerPlayers(players) {
        const container = $("#server-player-list");
        if (!container) return;

        container.replaceChildren();

        if (!players || players.length === 0) {
            const empty = document.createElement("p");
            empty.className = "muted";
            empty.textContent = "No players are currently online.";
            container.append(empty);
            return;
        }

        players.forEach(player => {
            const row = document.createElement("div");
            row.className = "server-player-row";

            const name = document.createElement("strong");
            name.textContent = player.name;

            const details = document.createElement("small");
            details.textContent = `${player.world} · ${player.gamemode}`;

            row.append(name, details);
            container.append(row);
        });
    }

    async function refreshServerOverview() {
        try {
            const data = await SSMPApi.server();

            const setText = (selector, value) => {
                const element = $(selector);
                if (element) element.textContent = value;
            };

            setText("#server-name", data.serverName || "Minecraft Server");
            setText("#server-motd", data.motd || "No MOTD configured.");
            setText("#server-online", data.onlinePlayers ?? 0);
            setText("#server-player-limit", `Maximum ${data.maxPlayers ?? "—"} players`);
            setText("#server-total-players", data.totalPlayers ?? "—");

            renderServerPlayers(data.players || []);

            const details = $("#server-details");
            if (!details) return;

            details.replaceChildren();

            const entries = [
                ["Server version", data.version],
                ["Bukkit version", data.bukkitVersion],
                ["Java version", data.javaVersion]
            ];

            entries.forEach(([label, value]) => {
                const row = document.createElement("div");
                row.className = "server-detail-row";

                const key = document.createElement("span");
                key.textContent = label;

                const result = document.createElement("strong");
                result.textContent = value || "Unknown";

                row.append(key, result);
                details.append(row);
            });
        } catch (error) {
            console.error("Server overview error:", error);
        }
    }

    function startResourceUpdates() {
        refreshResources();
        refreshServerOverview();

        resourceTimer = setInterval(() => {
            refreshResources();
            refreshServerOverview();
        }, 7000);
    }
    

    $$(".nav-item[data-view]").forEach(button => {
        button.addEventListener("click", async () => {
            setView(button.dataset.view);

            if (button.dataset.view === "modules") {
                await renderModules();
            }
        });
    });

    const themeToggle = $("#theme-toggle");

    if (themeToggle) {
        themeToggle.addEventListener("click", () => {
            const next = document.documentElement.dataset.theme === "dark"
                ? "light"
                : "dark";

            document.documentElement.dataset.theme = next;
            localStorage.setItem("ssmp-theme", next);
        });
    }

    const savedTheme = localStorage.getItem("ssmp-theme");
    if (savedTheme) document.documentElement.dataset.theme = savedTheme;

    const logoutButton = $("#logout");

    if (logoutButton) {
        logoutButton.addEventListener("click", async () => {
            try {
                await SSMPApi.logout();

                if (monitorTimer !== null) clearInterval(monitorTimer);
                if (resourceTimer !== null) clearInterval(resourceTimer);

                monitorTimer = null;
                resourceTimer = null;
            } finally {
                if (consoleStream) consoleStream.close();
                location.replace("/");
            }
        });
    }

    const updateButton = $("#update-check");

    if (updateButton) {
        updateButton.addEventListener("click", async event => {
            event.currentTarget.disabled = true;

            try {
                await SSMPApi.checkUpdates();
                toast("Update check started.");
            } catch (error) {
                toast(error.message);
            } finally {
                event.currentTarget.disabled = false;
            }
        });
    }

    const addDialog = $("#add-dialog");
    const addClose = $("#add-close");
    const addCancel = $("#add-cancel");
    const addKeyOpen = $("#add-key-open");

    if (addClose && addDialog) {
        addClose.addEventListener("click", () => addDialog.close());
    }

    if (addCancel && addDialog) {
        addCancel.addEventListener("click", () => addDialog.close());
    }

    if (addKeyOpen && addDialog) {
        addKeyOpen.addEventListener("click", () => {
            if (!currentFile) return;

            const error = $("#add-error");
            if (error) error.textContent = "";

            addDialog.showModal();
        });
    }

    const addForm = $("#add-form");

    if (addForm) {
        addForm.addEventListener("submit", async event => {
            event.preventDefault();

            const form = new FormData(event.currentTarget);

            try {
                await SSMPApi.add(
                    currentFile,
                    form.get("key"),
                    form.get("value"),
                    form.get("type")
                );

                if (addDialog) addDialog.close();
                event.currentTarget.reset();

                await loadFile(currentFile);
                toast("YAML key added.");
            } catch (error) {
                const addError = $("#add-error");
                if (addError) addError.textContent = error.message;
            }
        });
    }

    const passwordForm = $("#password-form");

    if (passwordForm) {
        passwordForm.addEventListener("submit", async event => {
            event.preventDefault();

            const form = new FormData(event.currentTarget);
            const result = $("#password-result");

            try {
                await SSMPApi.changePassword(
                    form.get("current"),
                    form.get("next")
                );

                if (result) {
                    result.textContent = "Password changed. Please sign in again.";
                }

                await SSMPApi.logout();
                setTimeout(() => location.replace("/"), 700);
            } catch (error) {
                if (result) result.textContent = error.message;
            }
        });
    }

    const consoleForm = $("#console-form");

    if (consoleForm) {
        consoleForm.addEventListener("submit", event => {
            event.preventDefault();

            const commandInput = $("#console-command");
            if (commandInput) sendConsoleCommand(commandInput.value);
        });
    }

    const consoleClear = $("#console-clear");

    if (consoleClear) {
        consoleClear.addEventListener("click", () => {
            const output = $("#console-output");
            if (output) output.replaceChildren();

            consoleLineCount = 0;
        });
    }

    refreshFiles().catch(error => toast(error.message));
    connectConsole();
    startResourceUpdates();
    startMonitorUpdates();

    window.addEventListener("resize", renderMonitorGraphs);
})();