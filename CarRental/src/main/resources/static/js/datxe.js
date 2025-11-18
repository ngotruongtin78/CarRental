// ===============================
// LẤY stationId
// ===============================
function getStationId() {
    const params = new URLSearchParams(window.location.search);
    return params.get("stationId");
}

let selectedStation = getStationId();
let selectedVehicle = null;
let stationCache = [];
let userCoordinates = null;

function initDates() {
    const today = new Date().toISOString().split("T")[0];
    const startInput = document.getElementById("start-date");
    const endInput = document.getElementById("end-date");
    if (startInput) startInput.value = today;
    if (endInput) endInput.value = today;
}

function requestUserLocation() {
    if (!navigator.geolocation) return;
    navigator.geolocation.getCurrentPosition(pos => {
        userCoordinates = {
            lat: pos.coords.latitude,
            lon: pos.coords.longitude
        };
        updateDistanceDisplay();
    });
}

function calculateDistanceKm(lat1, lon1, lat2, lon2) {
    const R = 6371;
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLon = (lon2 - lon1) * Math.PI / 180;
    const a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
        Math.sin(dLon / 2) * Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return Math.round(R * c * 10) / 10; // 1 decimal
}

function updateDistanceDisplay() {
    const distanceEl = document.getElementById("distance-display");
    if (!distanceEl) return;

    if (!selectedStation || !userCoordinates) {
        distanceEl.innerText = "Chưa xác định";
        return;
    }

    const station = stationCache.find(s => s.id === selectedStation);
    if (!station) {
        distanceEl.innerText = "Chưa xác định";
        return;
    }

    const km = calculateDistanceKm(userCoordinates.lat, userCoordinates.lon, station.latitude, station.longitude);
    distanceEl.innerText = `${km} km`;
    distanceEl.dataset.value = km;
}

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
        stationCache = stations;
        const container = document.getElementById("station-list");
        container.innerHTML = "";

        stations.forEach(st => {
            const div = document.createElement("div");
            div.classList.add("station-item");

            div.innerHTML = `
                <i class="fa-solid fa-location-dot"></i>
                <b>${st.name}</b>
                <small style="display:block;color:#555;">${st.address ?? ""}</small>
            `;

            div.onclick = () => {
                selectedStation = st.id;
                selectedVehicle = null;

                document.getElementById("selected-station").innerText = `${st.name} - ${st.address ?? ""}`;

                document.querySelectorAll(".station-item")
                    .forEach(el => el.classList.remove("active-station"));

                div.classList.add("active-station");

                loadVehicles();
                updateDistanceDisplay();
            };

            container.appendChild(div);
        });

        // ===============================
        // AUTO CHỌN TRẠM
        // ===============================
        if (selectedStation) {
            const stObj = stations.find(s => s.id === selectedStation);

            if (stObj) {
                document.getElementById("selected-station").innerText = `${stObj.name} - ${stObj.address ?? ""}`;

                document.querySelectorAll(".station-item")
                    .forEach(el => {
                        if (el.querySelector("b").innerText === stObj.name) {
                            el.classList.add("active-station");
                            el.scrollIntoView({ behavior: "smooth", block: "center" });
                        }
                    });

                loadVehicles();
                updateDistanceDisplay();
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
                    <h3>${v.brand ? v.brand : ""} ${v.type} (${v.plate})</h3>
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

    if (!selectedStation) {
        alert("Vui lòng chọn điểm thuê trước khi đặt!");
        return;
    }

    try {
        const form = new FormData();
        form.append("vehicleId", selectedVehicle);
        form.append("stationId", selectedStation ?? "");
        form.append("startDate", document.getElementById("start-date").value);
        form.append("endDate", document.getElementById("end-date").value);

        const distanceValue = document.getElementById("distance-display")?.dataset?.value;
        if (distanceValue) {
            form.append("distanceKm", distanceValue);
        }

        const res = await fetch("/api/rental/book", {
            method: "POST",
            body: form
        });

        const text = await res.text();

        try {
            const rental = JSON.parse(text);

            if (!res.ok) {
                alert(rental);
                return;
            }

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
initDates();
requestUserLocation();
loadStations();
