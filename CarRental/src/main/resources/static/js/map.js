let stations = [];
let userLat = null;
let userLng = null;
let router = null;

let map;

function initMap() {
    map = L.map("map").setView([10.80, 106.72], 14);

    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png")
        .addTo(map);

    getUserLocation();
}

function getUserLocation() {
    navigator.geolocation.getCurrentPosition(
        pos => {
            userLat = pos.coords.latitude;
            userLng = pos.coords.longitude;

            const userMarker = L.marker([userLat, userLng], {
                icon: L.icon({
                    iconUrl: "https://cdn-icons-png.flaticon.com/512/684/684908.png",
                    iconSize: [32, 32]
                })
            });

            userMarker.addTo(map)
                .bindPopup("📍 Vị trí của bạn")
                .openPopup();

            loadStations();
        },
        () => showToast("Bạn từ chối quyền truy cập vị trí!")
    );
}

function loadStations() {
    fetch("/api/stations")
        .then(res => res.json())
        .then(data => {
            stations = data;
            renderStations();
        })
        .catch(() => showToast("Lỗi tải danh sách trạm!"));
}

// Hàm renderStations đã được sửa đổi để hiển thị bố cục dọc
function renderStations() {
    const list = document.getElementById("stationList");
    list.innerHTML = "";

    stations.forEach(st => {
        const stationId = st.id;

        // Tính toán khoảng cách và thời gian
        st.distance = haversine(userLat, userLng, st.latitude, st.longitude);
        st.eta = Math.round((st.distance / 30) * 60); // Giả sử tốc độ 30km/h

        // Random offset tọa độ marker để tránh đè nhau (nếu cần)
        const offset = 0.00015;
        const lat = st.latitude + (Math.random() - 0.5) * offset;
        const lng = st.longitude + (Math.random() - 0.5) * offset;

        // --- GIAO DIỆN DANH SÁCH MỚI ---
        list.innerHTML += `
            <div class="location-item"
                 onclick="openStation(${lat}, ${lng}, '${stationId}', \`${st.name}\`, ${st.distance.toFixed(2)}, ${st.availableCars}, ${st.eta})">

                <h4>${st.name}</h4>

                <div class="stat-row">
                    <i class="fa-solid fa-location-dot"></i>
                    <span>${st.distance.toFixed(2)} km</span>
                </div>

                <div class="stat-row">
                    <i class="fa-solid fa-car"></i>
                    <span class="car-count">${st.availableCars} xe có sẵn</span>
                </div>

                <div class="stat-row" style="font-size: 13px; color: #888;">
                    <i class="fa-solid fa-clock" style="color: #888;"></i>
                    <span>~${st.eta} phút di chuyển</span>
                </div>
            </div>
        `;

        // --- MARKER TRÊN BẢN ĐỒ ---
        const marker = L.marker([lat, lng]).addTo(map);

        marker.bindPopup(`
            <div style="text-align:center;">
                <b style="font-size:14px">${st.name}</b><br>
                <hr style="margin:5px 0; border:0; border-top:1px solid #eee;">
                📏 ${st.distance.toFixed(2)} km &nbsp;|&nbsp; 🚗 ${st.availableCars} xe<br>
                ⏱ ${st.eta} phút<br><br>

                <button style="padding:5px 10px; background:#007bff; color:white; border:none; border-radius:4px; margin-right:5px; cursor:pointer;"
                        onclick="routeTo(${lat}, ${lng}); event.stopPropagation();">
                    🔄 Chỉ đường
                </button>

                <button style="padding:5px 10px; background:#388e3c; color:white; border:none; border-radius:4px; cursor:pointer;"
                        onclick="goToBooking('${stationId}'); event.stopPropagation();">
                    🚲 Đặt xe
                </button>
            </div>
        `);
    });
}

function openStation(lat, lng, stationId, name, distance, availableCars, eta) {
    map.setView([lat, lng], 16);

    L.popup()
        .setLatLng([lat, lng])
        .setContent(`
            <div style="text-align:center;">
                <b style="font-size:14px">${name}</b><br>
                <hr style="margin:5px 0; border:0; border-top:1px solid #eee;">
                📏 ${distance} km &nbsp;|&nbsp; 🚗 ${availableCars} xe<br>
                ⏱ ${eta} phút<br><br>

                <button style="padding:5px 10px; background:#007bff; color:white; border:none; border-radius:4px; margin-right:5px; cursor:pointer;"
                        onclick="routeTo(${lat}, ${lng}); event.stopPropagation();">
                    🔄 Chỉ đường
                </button>

                <button style="padding:5px 10px; background:#388e3c; color:white; border:none; border-radius:4px; cursor:pointer;"
                        onclick="goToBooking('${stationId}'); event.stopPropagation();">
                    🚲 Đặt xe
                </button>
            </div>
        `)
        .openOn(map);
}

function routeTo(lat, lng) {
    if (router) map.removeControl(router);

    router = L.Routing.control({
        waypoints: [
            L.latLng(userLat, userLng),
            L.latLng(lat, lng)
        ],
        routeWhileDragging: false,
        createMarker: () => null,
        lineOptions: { styles: [{ color: '#007bff', weight: 5 }] }
    }).addTo(map);
}

function goToBooking(stationId) {
    window.location.href = `/datxe?stationId=${stationId}`;
}

function haversine(lat1, lon1, lat2, lon2) {
    const R = 6371;
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLon = (lon2 - lon1) * Math.PI / 180;

    const a =
        Math.sin(dLat / 2) ** 2 +
        Math.cos(lat1 * Math.PI / 180) *
        Math.cos(lat2 * Math.PI / 180) *
        Math.sin(dLon / 2) ** 2;

    return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

function showToast(msg) {
    const t = document.getElementById("toast");
    if (!t) {
        console.warn("Toast element not found!");
        return;
    }
    t.innerHTML = msg;
    t.className = "toast show";

    setTimeout(() => t.className = "toast hidden", 3000);
}

initMap();