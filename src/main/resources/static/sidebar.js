document.addEventListener("DOMContentLoaded", function () {
    const hamburger = document.getElementById("hamburger-icon");
    const sidebar = document.querySelector(".sidebar");

    hamburger.addEventListener("click", function () {
        sidebar.classList.toggle("show-menu"); // Åpne/lukke menyen
    });

    // Klikk utenfor menyen for å lukke den
    document.addEventListener("click", function (event) {
        if (!sidebar.contains(event.target) && !hamburger.contains(event.target)) {
            sidebar.classList.remove("show-menu");
        }
    });
});


function scrollLeft() {
    document.querySelector(".liste").scrollBy({left: -350, behavior: "smooth"});  /* Increased from 300 to 350 */
}

function scrollRight() {
    document.querySelector(".liste").scrollBy({left: 350, behavior: "smooth"});   /* Increased from 300 to 350 */
}

document.addEventListener('DOMContentLoaded', function () {
    const leftBtn = document.querySelector(".scroll-btn.left");
    const rightBtn = document.querySelector(".scroll-btn.right");

    leftBtn.addEventListener('click', scrollLeft);
    rightBtn.addEventListener('click', scrollRight);
});
