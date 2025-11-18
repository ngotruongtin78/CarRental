let historyData = [];

function formatDate(dateStr) {
    if (!dateStr) return "";
    const date = new Date(dateStr);
    return `${date.toLocaleDateString()}`;
}

function renderHistoryItem(item) {
    const record = item.record || {};
    const vehicle = item.vehicle;
    const station = item.station;
    const container = document.createElement("div");
    container.classList.add("history-item");

    const start = formatDate(record.startDate || record.startTime);
    const end = formatDate(record.endDate || record.endTime) || "Chưa trả";
    const vehicleLabel = vehicle ? `${vehicle.brand ?? vehicle.type} (${vehicle.plate})` : record.vehicleId;
    const stationLabel = station ? `${station.name} - ${station.address ?? ""}` : (record.stationId || "");
    const distance = record.distanceKm ? `${Number(record.distanceKm).toFixed(1)} km` : "-";
    const total = record.total ? Number(record.total).toLocaleString("vi-VN") + " VNĐ" : "0";
    const displayStatus = item.displayStatus || record.displayStatus || record.status || "";

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
                <p>Trạng thái: ${displayStatus}</p>
            </div>
            <div class="detail-group">
                <i class="fas fa-dollar-sign"></i>
                <p>Chi phí: ${total}</p>
            </div>
            <div class="detail-group">
                <i class="fas fa-road"></i>
                <p>Quãng đường: ${distance}</p>
            </div>
        </div>
    `;

    return container;
}

function renderHistoryList(list) {
    const listEl = document.getElementById("history-list");
    if (!list || !list.length) {
        listEl.innerHTML = "<p>Chưa có chuyến thuê nào.</p>";
        updateAnalyticsFromHistory([]);
        return;
    }

    listEl.innerHTML = "";
    list.forEach(item => listEl.appendChild(renderHistoryItem(item)));
    updateAnalyticsFromHistory(list);
}

function parseDate(input) {
    if (!input) return null;
    const dt = new Date(input);
    return isNaN(dt.getTime()) ? null : dt;
}

function matchesVehicleType(vehicleType, filterValue) {
    const type = (vehicleType || "").toLowerCase();
    switch (filterValue) {
        case "oto4":
            return type.includes("4") || type.includes("4 ch");
        case "oto7":
            return type.includes("7") || type.includes("7 ch");
        case "xe_may_dien":
            return type.includes("xe m") || type.includes("máy");
        case "xe_tay_ga_dien":
            return type.includes("tay ga") || type.includes("ga");
        case "xe_dap_dien":
            return type.includes("đạp");
        default:
            return true;
    }
}

function filterHistory() {
    const period = document.getElementById("period-filter").value;
    const vehicleType = document.getElementById("vehicle-type-filter").value;
    const status = document.getElementById("status-filter").value;

    const now = new Date();
    const startBoundary = (() => {
        switch (period) {
            case "last7days":
                return new Date(now.getFullYear(), now.getMonth(), now.getDate() - 7);
            case "last30days":
                return new Date(now.getFullYear(), now.getMonth(), now.getDate() - 30);
            case "thismonth":
                return new Date(now.getFullYear(), now.getMonth(), 1);
            case "lastmonth":
                return new Date(now.getFullYear(), now.getMonth() - 1, 1);
            case "thisyear":
                return new Date(now.getFullYear(), 0, 1);
            default:
                return null;
        }
    })();

    const filtered = historyData.filter(item => {
        const record = item.record || {};
        const startDate = parseDate(record.startDate || record.startTime);
        const displayStatus = (item.displayStatus || record.displayStatus || record.status || "").toLowerCase();

        if (startBoundary && startDate && startDate < startBoundary) return false;

        if (period === "lastmonth") {
            const lastMonthStart = new Date(now.getFullYear(), now.getMonth() - 1, 1);
            const lastMonthEnd = new Date(now.getFullYear(), now.getMonth(), 0);
            if (!startDate || startDate < lastMonthStart || startDate > lastMonthEnd) return false;
        }

        if (vehicleType !== "all" && !matchesVehicleType(item.vehicle?.type, vehicleType)) return false;

        if (status !== "all") {
            if (status === "paid" && !displayStatus.includes("thanh toán")) return false;
            if (status === "active" && !displayStatus.includes("đang thuê")) return false;
            if (status === "returned" && !displayStatus.includes("trả xe")) return false;
        }

        return true;
    });

    renderHistoryList(filtered);
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
        historyData = await res.json();
        renderHistoryList(historyData);
    } catch (err) {
        console.error("Lỗi loadHistory:", err);
        listEl.innerHTML = "<p>Không tải được lịch sử thuê.</p>";
    }
}

function updateAnalyticsFromHistory(list) {
    const totalTrips = list.length;
    const totalSpent = list.reduce((sum, item) => sum + (item.record?.total || 0), 0);
    const durations = list
        .map(item => {
            const start = parseDate(item.record?.startTime);
            const end = parseDate(item.record?.endTime);
            return start && end ? (end - start) / 60000 : null;
        })
        .filter(v => v !== null);

    const avgDuration = durations.length
        ? durations.reduce((a, b) => a + b, 0) / durations.length
        : 0;

    document.getElementById("total-trips").innerText = totalTrips;
    document.getElementById("total-spent").innerHTML = `${totalSpent.toLocaleString("vi-VN")} <small>VNĐ</small>`;
    document.getElementById("avg-duration").innerHTML = `${Math.round(avgDuration)} <small>phút</small>`;
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

    document.querySelector(".btn-filter")?.addEventListener("click", filterHistory);
    document.getElementById("period-filter")?.addEventListener("change", filterHistory);
    document.getElementById("vehicle-type-filter")?.addEventListener("change", filterHistory);
    document.getElementById("status-filter")?.addEventListener("change", filterHistory);
});
