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
