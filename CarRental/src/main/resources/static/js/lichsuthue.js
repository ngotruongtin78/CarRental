let historyData = [];

const rentalModal = {
    el: null,
    body: null,
    badges: null,
    title: null,
};

function formatDate(dateStr) {
    if (!dateStr) return "";
    const date = new Date(dateStr);
    return `${date.toLocaleDateString()}`;
}

function formatDateTime(dateStr) {
    if (!dateStr) return "";
    const date = new Date(dateStr);
    if (isNaN(date.getTime())) return "";
    return `${date.toLocaleDateString()} ${date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}`;
}

function formatMoney(value) {
    if (value === undefined || value === null || isNaN(value)) return "---";
    return `${Number(value).toLocaleString("vi-VN")} VNĐ`;
}

function isCancelled(record) {
    const status = (record.status || "").toUpperCase();
    const paymentStatus = (record.paymentStatus || "").toUpperCase();
    return ["CANCELLED", "EXPIRED"].includes(status) || ["CANCELLED", "EXPIRED"].includes(paymentStatus);
}

function isCompleted(record) {
    const status = (record.status || "").toUpperCase();
    return ["COMPLETED", "RETURNED"].includes(status);
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
    const total = record.total ? Number(record.total).toLocaleString("vi-VN") + " VNĐ" : "0";
    const displayStatus = item.displayStatus || record.displayStatus || record.status || "";
    const distance = record.distanceKm ? `${record.distanceKm.toFixed(1)} km` : "Chưa có";

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
                <i class="fas fa-road"></i>
                <p>Quãng đường tới trạm: ${distance}</p>
            </div>
            <div class="detail-group">
                <i class="fas fa-dollar-sign"></i>
                <p>Chi phí: ${total}</p>
            </div>
        </div>
    `;

    const actions = document.createElement("div");
    actions.classList.add("history-actions");

    const statusBadge = document.createElement("span");
    statusBadge.classList.add("status-badge");
    statusBadge.innerText = `Trạng thái: ${displayStatus}`;
    const statusUpper = (record.status || "").toUpperCase();
    if (displayStatus.toLowerCase().includes("chờ thanh toán") || statusUpper === "PENDING_PAYMENT") {
        statusBadge.classList.add("warning");
    } else if (statusUpper === "WAITING_INSPECTION") {
        statusBadge.classList.add("warning");
    }
    actions.appendChild(statusBadge);

    const disabled = isCancelled(record) || isCompleted(record);

    if (!disabled && !record.contractSigned) {
        const btn = document.createElement("button");
        btn.className = "action-button";
        btn.innerHTML = '<i class="fas fa-file-signature"></i> Ký hợp đồng điện tử';
        btn.onclick = () => signContract(record.id);
        actions.appendChild(btn);
    }

    if (!disabled) {
        const btnCheckin = document.createElement("button");
        btnCheckin.className = "action-button";
        btnCheckin.innerHTML = '<i class="fas fa-check"></i> Check-in nhận xe';
        btnCheckin.onclick = () => checkIn(record.id);
        actions.appendChild(btnCheckin);
    }

    if (!disabled && statusUpper !== "WAITING_INSPECTION") {
        const btnReturn = document.createElement("button");
        btnReturn.className = "action-button secondary";
        btnReturn.innerHTML = '<i class="fas fa-undo"></i> Yêu cầu trả xe';
        btnReturn.onclick = () => requestReturn(record.id);
        actions.appendChild(btnReturn);
    } else if (statusUpper === "WAITING_INSPECTION") {
        const note = document.createElement("span");
        note.className = "status-badge warning";
        note.innerText = "Đã gửi yêu cầu trả xe";
        actions.appendChild(note);
    }

    container.appendChild(actions);

    if (record.checkinNotes) {
        const p = document.createElement("p");
        p.className = "note-text";
        p.innerText = `Ghi chú check-in: ${record.checkinNotes}`;
        container.appendChild(p);
    }
    if (record.returnNotes) {
        const p = document.createElement("p");
        p.className = "note-text";
        p.innerText = `Ghi chú trả xe: ${record.returnNotes}`;
        container.appendChild(p);
    }

    const btnDetails = document.createElement("button");
    btnDetails.className = "btn-view-details";
    btnDetails.innerHTML = '<i class="fas fa-circle-info"></i> Xem chi tiết chuyến';
    btnDetails.onclick = (e) => {
        e.stopPropagation();
        openRentalModal(item);
    };
    container.appendChild(btnDetails);

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

function deriveFilterStatus(item) {
    const record = item.record || {};
    const directFilter = (item.filterStatus || record.filterStatus || "").toLowerCase();
    if (directFilter) return directFilter;

    const display = (item.displayStatus || record.displayStatus || record.status || "").toLowerCase();
    if (display.includes("trả xe")) return "returned";
    if (display.includes("đang thuê")) return "active";
    if (display.includes("chờ thanh toán") || display.includes("đã thuê")) return "rented";
    return "";
}

function filterHistory() {
    const period = document.getElementById("period-filter").value;
    const vehicleType = document.getElementById("vehicle-type-filter").value;
    const status = document.getElementById("status-filter").value;

    const now = new Date();
    const periodRange = (() => {
        switch (period) {
            case "last7days":
                return { start: new Date(now.getFullYear(), now.getMonth(), now.getDate() - 7), end: now };
            case "last30days":
                return { start: new Date(now.getFullYear(), now.getMonth(), now.getDate() - 30), end: now };
            case "thismonth":
                return { start: new Date(now.getFullYear(), now.getMonth(), 1), end: now };
            case "lastmonth":
                return {
                    start: new Date(now.getFullYear(), now.getMonth() - 1, 1),
                    end: new Date(now.getFullYear(), now.getMonth(), 0)
                };
            case "thisyear":
                return { start: new Date(now.getFullYear(), 0, 1), end: now };
            default:
                return { start: null, end: null };
        }
    })();

    const filtered = historyData.filter(item => {
        const record = item.record || {};
        const startDate = parseDate(record.startDate || record.startTime);
        const filterStatus = deriveFilterStatus(item);

        if (periodRange.start && (!startDate || startDate < periodRange.start)) return false;
        if (periodRange.end && startDate && startDate > periodRange.end) return false;

        if (vehicleType !== "all" && !matchesVehicleType(item.vehicle?.type, vehicleType)) return false;

        if (status !== "all") {
            if (filterStatus !== status) return false;
        }

        return true;
    });

    renderHistoryList(filtered);
}

async function signContract(rentalId) {
    try {
        const res = await fetch(`/api/rental/${rentalId}/sign-contract`, { method: "POST" });
        const txt = await res.text();
        if (!res.ok) return alert(txt || "Không ký được hợp đồng");
        alert("Đã ký hợp đồng điện tử. Vui lòng check-in để nhận xe.");
        loadHistory();
    } catch (e) {
        alert("Không ký được hợp đồng. Thử lại sau.");
    }
}

async function checkIn(rentalId) {
    const notes = prompt("Nhập ghi chú tình trạng xe khi nhận (tuỳ chọn):", "");
    try {
        const res = await fetch(`/api/rental/${rentalId}/check-in`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ notes: notes || "" })
        });
        const txt = await res.text();
        if (!res.ok) return alert(txt || "Check-in thất bại");
        alert("Đã check-in. Hãy chụp ảnh và lưu ý với nhân viên tại quầy.");
        loadHistory();
    } catch (e) {
        alert("Không check-in được. Thử lại sau.");
    }
}

async function requestReturn(rentalId) {
    const notes = prompt("Nhập ghi chú khi trả xe (hư hỏng, tình trạng...):", "");
    try {
        const res = await fetch(`/api/rental/${rentalId}/return`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ notes: notes || "" })
        });
        const txt = await res.text();
        if (!res.ok) return alert(txt || "Không gửi được yêu cầu trả xe");
        alert("Đã yêu cầu trả xe. Vui lòng bàn giao tại điểm thuê để nhân viên kiểm tra.");
        loadHistory();
    } catch (e) {
        alert("Không gửi được yêu cầu trả xe. Thử lại sau.");
    }
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
    const totalDistance = list.reduce((sum, item) => sum + (item.record?.distanceKm || 0), 0);
    const durations = list
        .map(item => {
            const start = parseDate(item.record?.startTime);
            const end = parseDate(item.record?.endTime);
            return start && end ? (end - start) / 60000 : null;
        })
        .filter(v => v !== null);

    const avgDurationDays = durations.length
        ? (durations.reduce((a, b) => a + b, 0) / durations.length) / 1440
        : 0;

    document.getElementById("total-trips").innerText = totalTrips;
    document.getElementById("total-spent").innerHTML = `${totalSpent.toLocaleString("vi-VN")} <small>VNĐ</small>`;
    document.getElementById("avg-duration").innerHTML = `${avgDurationDays.toFixed(1)} <small>ngày</small>`;
    document.getElementById("total-distance").innerHTML = `${totalDistance.toFixed(1)} <small>km</small>`;
}

async function loadStats() {
    try {
        const res = await fetch("/api/rental/stats");
        if (!res.ok) return;
        const stats = await res.json();

        document.getElementById("total-trips").innerText = stats.totalTrips || 0;
        document.getElementById("total-spent").innerHTML = `${(stats.totalSpent || 0).toLocaleString("vi-VN")} <small>VNĐ</small>`;
        const avgDays = stats.averageDurationMinutes
            ? (stats.averageDurationMinutes / 1440).toFixed(1)
            : "0.0";
        document.getElementById("avg-duration").innerHTML = `${avgDays} <small>ngày</small>`;
        const distance = stats.totalDistance || 0;
        document.getElementById("total-distance").innerHTML = `${distance.toFixed(1)} <small>km</small>`;
        const peaks = stats.peakHours && stats.peakHours.length ? stats.peakHours.map(h => `${h}h`).join(", ") : "-";
        document.getElementById("peak-hours").innerText = peaks;
    } catch (err) {
        console.error("Lỗi loadStats:", err);
    }
}

function closeRentalModal() {
    if (rentalModal.el) rentalModal.el.classList.remove("show");
}

function renderBadges(item) {
    if (!rentalModal.badges) return;
    rentalModal.badges.innerHTML = "";
    const record = item.record || {};
    const badges = [];

    const statusText = item.displayStatus || record.displayStatus || record.status;
    if (statusText) badges.push({ text: statusText, className: "" });

    if (record.paymentStatus) badges.push({ text: `Thanh toán: ${record.paymentStatus}`, className: record.paymentStatus.toUpperCase().includes("PENDING") ? "warning" : "" });
    if (record.paymentMethod) badges.push({ text: `PTTT: ${record.paymentMethod}`, className: "" });

    badges.forEach(b => {
        const el = document.createElement("span");
        el.className = `modal-badge${b.className ? " " + b.className : ""}`;
        el.innerText = b.text;
        rentalModal.badges.appendChild(el);
    });
}

function buildDetailSection(title, rows) {
    if (!rows || !rows.length) return "";
    const content = rows.map(r => `<div class="detail-row"><span>${r.label}</span><strong>${r.value || "---"}</strong></div>`).join("");
    return `<div class="modal-section"><h4>${title}</h4><div class="detail-grid">${content}</div></div>`;
}

function openRentalModal(item) {
    if (!rentalModal.el || !rentalModal.body || !rentalModal.title) return;
    const record = item.record || {};
    const vehicle = item.vehicle || {};
    const station = item.station || {};

    rentalModal.title.textContent = vehicle.brand ? `${vehicle.brand} (${vehicle.plate || ""})` : `Chuyến #${record.id}`;
    renderBadges(item);

    const rentalRows = [
        { label: "Mã chuyến", value: `#${record.id || ""}` },
        { label: "Thời gian thuê", value: `${formatDate(record.startDate || record.startTime)} - ${formatDate(record.endDate || record.endTime) || "Chưa trả"}` },
        { label: "Số ngày", value: record.rentalDays ? `${record.rentalDays} ngày` : "---" },
        { label: "Tổng phí", value: formatMoney(record.total) },
        { label: "Trạng thái", value: item.displayStatus || record.status || "" },
        { label: "Thanh toán", value: record.paymentStatus || "Chưa cập nhật" },
        { label: "PT thanh toán", value: record.paymentMethod || "---" },
        { label: "Giữ chỗ tới", value: record.holdExpiresAt ? formatDateTime(record.holdExpiresAt) : "---" },
    ];

    const vehicleRows = [
        { label: "Loại xe", value: vehicle.type || "---" },
        { label: "Biển số", value: vehicle.plate || "---" },
        { label: "Thương hiệu", value: vehicle.brand || "---" },
        { label: "Giá / ngày", value: vehicle.price ? formatMoney(vehicle.price) : "---" },
        { label: "Khoảng cách đến trạm", value: record.distanceKm ? `${record.distanceKm.toFixed(1)} km` : "---" },
    ];

    const stationRows = [
        { label: "Điểm thuê", value: station.name || record.stationId || "---" },
        { label: "Địa chỉ", value: station.address || "---" },
    ];

    const notes = [];
    if (record.checkinNotes) notes.push(`<div class="note-block"><strong>Ghi chú nhận xe:</strong><br>${record.checkinNotes}</div>`);
    if (record.returnNotes) notes.push(`<div class="note-block"><strong>Ghi chú trả xe:</strong><br>${record.returnNotes}</div>`);

    rentalModal.body.innerHTML = [
        buildDetailSection("Thông tin chuyến", rentalRows),
        buildDetailSection("Xe & chi phí", vehicleRows),
        buildDetailSection("Trạm thuê", stationRows),
        notes.length ? `<div class="modal-section">${notes.join("")}</div>` : "",
    ].join("");

    rentalModal.el.classList.add("show");
}

function initRentalModal() {
    rentalModal.el = document.getElementById("rental-modal");
    rentalModal.body = document.getElementById("modal-body");
    rentalModal.badges = document.getElementById("modal-badges");
    rentalModal.title = document.getElementById("modal-title");

    const overlay = rentalModal.el?.querySelector(".modal-overlay");
    const closeBtn = rentalModal.el?.querySelector(".modal-close");
    overlay?.addEventListener("click", closeRentalModal);
    closeBtn?.addEventListener("click", closeRentalModal);
    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape" && rentalModal.el?.classList.contains("show")) closeRentalModal();
    });
}

window.addEventListener("DOMContentLoaded", () => {
    loadHistory();
    loadStats();
    initRentalModal();

    document.querySelector(".btn-filter")?.addEventListener("click", filterHistory);
    document.getElementById("period-filter")?.addEventListener("change", filterHistory);
    document.getElementById("vehicle-type-filter")?.addEventListener("change", filterHistory);
    document.getElementById("status-filter")?.addEventListener("change", filterHistory);
});
