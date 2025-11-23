(function () {
    function closeAllMenus(except) {
        document.querySelectorAll(".user-menu").forEach(menu => {
            if (menu !== except) {
                menu.classList.remove("open");
            }
        });
    }

    document.addEventListener("DOMContentLoaded", () => {
        document.querySelectorAll(".user-menu").forEach(menu => {
            menu.addEventListener("mouseenter", () => {
                closeAllMenus(menu);
                menu.classList.add("open");
            });

            menu.addEventListener("mouseleave", () => {
                menu.classList.remove("open");
            });

            menu.addEventListener("focusin", () => {
                closeAllMenus(menu);
                menu.classList.add("open");
            });

            menu.addEventListener("focusout", (evt) => {
                if (!menu.contains(evt.relatedTarget)) {
                    menu.classList.remove("open");
                }
            });
        });
    });
})();
