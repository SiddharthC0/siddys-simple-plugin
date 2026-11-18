document.getElementById("login-form").addEventListener("submit", async event => {
    event.preventDefault();
    const button = event.currentTarget.querySelector("button");
    const error = document.getElementById("login-error");
    error.textContent = "";
    button.disabled = true;
    button.textContent = "Checking…";
    try {
        await SSMPApi.login(document.getElementById("password").value);
        location.replace("/");
    } catch (exception) {
        error.textContent = exception.message;
        button.disabled = false;
        button.innerHTML = 'Unlock panel <span>↗</span>';
    }
});


function updateLoginClock() {
    const clock = document.getElementById("login-clock");
    const date = document.getElementById("login-date");

    if (!clock || !date) return;

    const now = new Date();

    clock.textContent = new Intl.DateTimeFormat("en-IN", {
        hour: "2-digit",
        minute: "2-digit",
        hour12: false,
    }).format(now);

    date.textContent = new Intl.DateTimeFormat("en-IN", {
        weekday: "long",
        day: "2-digit",
        month: "long",
    }).format(now).toUpperCase();
}

updateLoginClock();
setInterval(updateLoginClock, 1000);