// ===============================
// LẤY stationId
// ===============================
function getStationId() {
    const params = new URLSearchParams(window.location.search);
    return params.get("stationId");
}

let selectedStation = getStationId();
let selectedVehicle = null;

async function checkLogin() {
    try {
        const res = await fetch("/api/renter/profile");
        return res.ok;
    } catch {
        return false;
    }
}

// ===============================
// LOAD DANH SÁCH TRẠM
// ===============================
async function loadStations() {
    try {
        const res = await fetch("/api/stations");
        if (!res.ok) return console.error("Không gọi được /api/stations");

        const stations = await res.json();
        const container = document.getElementById("station-list");
        container.innerHTML = "";

        stations.forEach(st => {
            const div = document.createElement("div");
            div.classList.add("station-item");

            div.innerHTML = `
                <i class="fa-solid fa-location-dot"></i>
                <b>${st.name}</b>
            `;

            div.onclick = () => {
                selectedStation = st.id;
                selectedVehicle = null;

                document.getElementById("selected-station").innerText = st.name;

                document.querySelectorAll(".station-item")
                    .forEach(el => el.classList.remove("active-station"));

                div.classList.add("active-station");

                loadVehicles();
            };

            container.appendChild(div);
        });

        // ===============================
        // AUTO CHỌN TRẠM
        // ===============================
        if (selectedStation) {
            const stObj = stations.find(s => s.id === selectedStation);

            if (stObj) {
                document.getElementById("selected-station").innerText = stObj.name;

                document.querySelectorAll(".station-item")
                    .forEach(el => {
                        if (el.querySelector("b").innerText === stObj.name) {
                            el.classList.add("active-station");
                            el.scrollIntoView({ behavior: "smooth", block: "center" });
                        }
                    });

                loadVehicles();
            }
        }

    } catch (e) {
        console.error("Lỗi loadStations:", e);
    }
}

// ===============================
// LOAD XE THEO TRẠM
// ===============================
async function loadVehicles() {
    const list = document.getElementById("vehicle-list");

    if (!selectedStation) {
        list.innerHTML = "<p>Chưa chọn điểm thuê.</p>";
        return;
    }

    try {
        const res = await fetch(`/api/vehicles/station/${selectedStation}`);
        if (!res.ok) {
            list.innerHTML = "<p>Không tải được danh sách xe.</p>";
            return;
        }

        const vehicles = await res.json();
        list.innerHTML = "";

        if (!vehicles.length) {
            list.innerHTML = "<p>Không có xe nào tại trạm này.</p>";
            return;
        }

        vehicles.forEach(v => {
            const div = document.createElement("div");
            div.classList.add("vehicle-card");

            div.innerHTML = `
                <div class="vehicle-icon"><i class="fas fa-car"></i></div>
                <div>
                    <h3>${v.brand ? v.brand : ""} ${v.type}</h3>
                    <p>Biển số: <b>${v.plate}</b></p>
                    <p>Pin: ${v.battery}%</p>
                    <p class="price-text">${v.price.toLocaleString('vi-VN')}đ / ngày</p>
                </div>
            `;

            div.onclick = () => {
                selectedVehicle = v.id;

                document.querySelectorAll(".vehicle-card")
                    .forEach(el => el.classList.remove("selected"));

                div.classList.add("selected");
            };

            list.appendChild(div);
        });

    } catch (e) {
        console.error("Lỗi loadVehicles:", e);
        list.innerHTML = "<p>Có lỗi khi tải xe.</p>";
    }
}

// ===============================
// ĐẶT XE
// ===============================
async function bookNow() {
    const loggedIn = await checkLogin();
    if (!loggedIn) {
        alert("Bạn cần đăng nhập để đặt xe!");
        window.location.href = "/login";
        return;
    }

    if (!selectedVehicle) {
        alert("Bạn phải chọn 1 xe trước khi đặt!");
        return;
    }

    try {
        const form = new FormData();
        form.append("vehicleId", selectedVehicle);

        const res = await fetch("/api/rentals/book", {
            method: "POST",
            body: form
        });

        const text = await res.text();

        try {
            const rental = JSON.parse(text);

            if (!rental || !rental.id) {
                alert("Lỗi từ server!");
                return;
            }

            window.location.href = `/thanhtoan?rentalId=${rental.id}`;
        } catch (err) {
            alert(text);
        }

    } catch (e) {
        console.error("Lỗi khi đặt xe:", e);
        alert("Có lỗi khi đặt xe.");
    }
}

document.getElementById("btn-book").onclick = bookNow;

// ===============================
// KHỞI CHẠY
// ===============================
loadStations();
