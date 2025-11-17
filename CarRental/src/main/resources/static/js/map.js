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
        () => showToast("❌ Bạn từ chối quyền truy cập vị trí!")
    );
}

function loadStations() {
    fetch("/api/stations")
        .then(res => res.json())
        .then(data => {
            stations = data;
            renderStations();
        })
        .catch(() => showToast("❌ Lỗi tải danh sách trạm!"));
}
function renderStations() {

    const list = document.getElementById("stationList");
    list.innerHTML = "";

    stations.forEach(st => {
        const stationId = st.id;
        st.distance = haversine(userLat, userLng, st.latitude, st.longitude);
        st.eta = Math.round((st.distance / 30) * 60);
        const offset = 0.00015;
        const lat = st.latitude + (Math.random() - 0.5) * offset;
        const lng = st.longitude + (Math.random() - 0.5) * offset;

        list.innerHTML += `
            <div class="location-item"
                 onclick="openStation(${lat}, ${lng}, '${stationId}', \`${st.name}\`, ${st.distance.toFixed(2)}, ${st.availableCars}, ${st.eta})">

                <div class="location-item-header">
                    <span class="station-title">${st.name}</span>
                </div>
                <div class="location-details">
                    <span><i class="fa-solid fa-location-dot"></i> ${st.distance.toFixed(2)} km</span>
                    <span><i class="fa-solid fa-car"></i> ${st.availableCars} xe có sẵn</span>
                    <span><i class="fa-solid fa-clock"></i> ${st.eta} phút</span>
                </div>
            </div>
        `;

        const marker = L.marker([lat, lng]).addTo(map);

        marker.bindPopup(`
            <b style="font-size:14px">${st.name}</b><br>
            📏 ${st.distance.toFixed(2)} km<br>
            🚗 ${st.availableCars} xe<br>
            ⏱ ${st.eta} phút<br><br>

            <button style="padding:5px 10px"
                    onclick="routeTo(${lat}, ${lng}); event.stopPropagation();">
                🔄 Chỉ đường
            </button>

            <button style="padding:5px 10px; margin-left:8px"
                    onclick="goToBooking('${stationId}'); event.stopPropagation();">
                🚲 Đặt xe
            </button>
        `);
    });
}

function openStation(lat, lng, stationId, name, distance, availableCars, eta) {
    map.setView([lat, lng], 16);

    L.popup()
        .setLatLng([lat, lng])
        .setContent(`
            <b style="font-size:14px">${name}</b><br>
            📏 ${distance} km<br>
            🚗 ${availableCars} xe<br>
            ⏱ ${eta} phút<br><br>

            <button style="padding:5px 10px"
                    onclick="routeTo(${lat}, ${lng}); event.stopPropagation();">
                🔄 Chỉ đường
            </button>

            <button style="padding:5px 10px; margin-left:8px"
                    onclick="goToBooking('${stationId}'); event.stopPropagation();">
                🚲 Đặt xe
            </button>
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