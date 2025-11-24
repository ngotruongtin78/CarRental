document.addEventListener("DOMContentLoaded", function () {


    const userMenus = document.querySelectorAll(".user-menu");


    function closeAllMenus() {
        userMenus.forEach(menu => {
            menu.classList.remove("open");
        });
    }

    userMenus.forEach(menu => {
        const toggleBtn = menu.querySelector(".user-menu-toggle");

        if (toggleBtn) {

            toggleBtn.addEventListener("click", function (e) {

                e.stopPropagation();


                const isAlreadyOpen = menu.classList.contains("open");


                closeAllMenus();


                if (!isAlreadyOpen) {
                    menu.classList.add("open");
                }
            });
        }
    });


    window.addEventListener("click", function (e) {

        closeAllMenus();
    });
});