(function () {
    function closeAllMenus(except) {
        document.querySelectorAll(".user-menu").forEach(menu => {
            if (menu !== except) {
                menu.classList.remove("open");
            }
        });
    }

    document.addEventListener("DOMContentLoaded", () => {
        document.querySelectorAll(".user-menu-toggle").forEach(btn => {
            btn.addEventListener("click", (evt) => {
                evt.stopPropagation();
                const menu = btn.closest(".user-menu");
                if (!menu) return;
                const isOpen = menu.classList.contains("open");
                closeAllMenus(menu);
                if (!isOpen) {
                    menu.classList.add("open");
                }
            });
        });

        document.addEventListener("click", () => closeAllMenus());
    });
})();
