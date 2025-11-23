(document.addEventListener("DOMContentLoaded", () => {
    const usernameInput = document.getElementById("username");
    const passwordInput = document.getElementById("password");
    const rememberCheckbox = document.getElementById("remember-me");
    const loginForm = document.querySelector("form[action$='login-process']") || document.querySelector("form");
    const STORAGE_KEY = "evstation-login";

    try {
        const saved = localStorage.getItem(STORAGE_KEY);
        if (saved) {
            const data = JSON.parse(saved);
            if (data?.username && usernameInput) {
                usernameInput.value = data.username;
            }
            if (data?.password && passwordInput) {
                passwordInput.value = data.password;
            }
            if (rememberCheckbox && data?.remember) {
                rememberCheckbox.checked = true;
            }
        }
    } catch (e) {
        console.warn("Không thể tải thông tin đăng nhập đã lưu", e);
    }

    if (loginForm) {
        loginForm.addEventListener("submit", () => {
            if (!rememberCheckbox) return;

            if (rememberCheckbox.checked) {
                const payload = {
                    username: usernameInput?.value || "",
                    password: passwordInput?.value || "",
                    remember: true
                };
                try {
                    localStorage.setItem(STORAGE_KEY, JSON.stringify(payload));
                } catch (e) {
                    console.warn("Không thể lưu thông tin đăng nhập", e);
                }
            } else {
                localStorage.removeItem(STORAGE_KEY);
            }
        });
    }
}));
