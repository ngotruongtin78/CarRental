const vehicleCache = new Map();
const stationCache = new Map();

async function fetchJson(url) {
    const res = await fetch(url);
    if (!res.ok) throw new Error(`Request failed: ${res.status}`);
    return res.json();
}

async function getVehicle(id) {
    if (!id) return null;
    if (vehicleCache.has(id)) return vehicleCache.get(id);
    const data = await fetchJson(`/api/vehicles/admin/${id}`);
    const vehicle = data && data.type ? data : null;
    vehicleCache.set(id, vehicle);
    return vehicle;
}

async function getStation(id) {
    if (!id) return null;
    if (stationCache.has(id)) return stationCache.get(id);
    const data = await fetchJson(`/api/stations/admin/${id}`);
    const station = data && data.name ? data : null;
    stationCache.set(id, station);
    return station;
}

function formatDate(dateStr) {
    if (!dateStr) return "";
    const date = new Date(dateStr);
    return `${date.toLocaleDateString()} ${date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`;
}

function renderHistoryItem(record, vehicle, station) {
    const container = document.createElement("div");
    container.classList.add("history-item");

    const start = formatDate(record.startTime);
    const end = formatDate(record.endTime) || "Chưa trả";
    const vehicleLabel = vehicle ? `${vehicle.type} (${vehicle.plate})` : record.getVehicleId || record.vehicleId;
    const stationLabel = station ? station.name : (record.stationId || "");
    const total = record.total ? record.total.toLocaleString("vi-VN") + " VNĐ" : "0";

    container.innerHTML = `
        <div class="item-header">
            <span class="trip-id">#${record.id}</span>
            <span class="trip-date">${start}${end ? " - " + end : ""}</span>
        </div>
        <div class="item-details">
            <div class="detail-group">
                <i class="fas fa-motorcycle"></i>
                <p>Xe: ${vehicleLabel || "N/A"}</p>
            </div>
            <div class="detail-group">
                <i class="fas fa-map-marker-alt"></i>
                <p>Điểm thuê: ${stationLabel || ""}</p>
            </div>
            <div class="detail-group">
                <i class="fas fa-info-circle"></i>
                <p>Trạng thái: ${record.status || ""}</p>
            </div>
            <div class="detail-group">
                <i class="fas fa-dollar-sign"></i>
                <p>Chi phí: ${total}</p>
            </div>
        </div>
    `;

    return container;
}

async function loadHistory() {
    const listEl = document.getElementById("history-list");
    listEl.innerHTML = "<p>Đang tải...</p>";

    try {
        const res = await fetch("/api/rental/history");
        if (res.status === 401) {
            listEl.innerHTML = "<p>Bạn cần đăng nhập để xem lịch sử.</p>";
            return;
        }
        const data = await res.json();

        if (!data.length) {
            listEl.innerHTML = "<p>Chưa có chuyến thuê nào.</p>";
            return;
        }

        listEl.innerHTML = "";
        for (const record of data) {
            const [vehicle, station] = await Promise.all([
                getVehicle(record.vehicleId),
                getStation(record.stationId)
            ]);
            listEl.appendChild(renderHistoryItem(record, vehicle, station));
        }
    } catch (err) {
        console.error("Lỗi loadHistory:", err);
        listEl.innerHTML = "<p>Không tải được lịch sử thuê.</p>";
    }
}

async function loadStats() {
    try {
        const res = await fetch("/api/rental/stats");
        if (!res.ok) return;
        const stats = await res.json();

        document.getElementById("total-trips").innerText = stats.totalTrips || 0;
        document.getElementById("total-spent").innerHTML = `${(stats.totalSpent || 0).toLocaleString("vi-VN")} <small>VNĐ</small>`;
        document.getElementById("avg-duration").innerHTML = `${Math.round(stats.averageDurationMinutes || 0)} <small>phút</small>`;
    } catch (err) {
        console.error("Lỗi loadStats:", err);
    }
}

window.addEventListener("DOMContentLoaded", () => {
    loadHistory();
    loadStats();
});
