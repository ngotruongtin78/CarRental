let historyData = [];

const rentalModal = {
    el: null,
    body: null,
    badges: null,
    title: null,
};

const contractModalState = {
    el: null,
    body: null,
    acceptBtn: null,
};

const checkinModalState = {
    el: null,
    notesInput: null,
    photoInput: null,
    preview: null,
};

let pendingContractRentalId = null;
let contractAcceptedCallback = null;
let activeCheckinRecord = null;

const CONTRACT_HTML = `
    <p><strong>CHÍNH SÁCH THUÊ XE – EV RENTAL</strong></p>
    <p><strong>1. Mục đích</strong><br>Chính sách này quy định quyền lợi, nghĩa vụ và trách nhiệm của Người thuê xe và Đơn vị cung cấp dịch vụ EV Rental, nhằm đảm bảo trải nghiệm thuê xe an toàn – minh bạch – hiệu quả.</p>
    <p><strong>2. Điều kiện thuê xe</strong><br><strong>2.1. Yêu cầu đối với người thuê</strong><br>Công dân Việt Nam hoặc người nước ngoài có giấy tờ hợp lệ.<br>Đủ 18 tuổi trở lên.<br>Có CMND/CCCD/Hộ chiếu còn hiệu lực.<br>Có Giấy phép lái xe phù hợp (đối với xe máy ≥175cc hoặc ô tô).<br>Tài khoản đã được xác thực trên hệ thống hoặc qua trạm thuê.</p>
    <p><strong>2.2. Hồ sơ cần xuất trình</strong><br>CMND/CCCD bản gốc (hoặc hộ chiếu).<br>Bằng lái xe hợp lệ.<br>Thông tin liên hệ (số điện thoại, email).</p>
    <p><strong>3. Quy định nhận xe</strong><br>Người thuê đến đúng điểm thuê đã chọn.<br>Kiểm tra hiện trạng xe cùng nhân viên → lập biên bản bàn giao.<br>Kiểm tra: Thắng, đèn xe, lốp xe. Giấy tờ xe. Ký hợp đồng thuê hoặc xác nhận điện tử trên hệ thống. Nhận mũ bảo hiểm/thiết bị kèm theo (nếu có).</p>
    <p><strong>4. Quy định trong thời gian thuê</strong><br>Người thuê tự chịu trách nhiệm khi điều khiển xe.<br>Tuyệt đối không cho người khác thuê lại hoặc giao xe cho người không có GPLX.<br>Không sử dụng xe cho: Đua xe, chạy quá tốc độ quy định. Chở hàng cấm / hàng quá tải trọng. Mục đích phạm pháp.<br>Bảo quản xe, khóa xe khi dừng đỗ.<br>Báo ngay cho tổng đài nếu: xe hư, cháy nổ, tai nạn, mất giấy tờ…</p>
    <p><strong>5. Quy định trả xe</strong><br>Trả xe tại đúng điểm thuê ban đầu (hoặc điểm khác nếu hệ thống hỗ trợ).<br>Nhân viên kiểm tra: Tình trạng xe. Thiết bị đi kèm.<br>Quyết toán chi phí còn lại: Phí thuê. Phí phát sinh (nếu có). Hoàn lại tiền cọc (nếu đủ điều kiện).</p>
    <p><strong>6. Chính sách giá & thanh toán</strong><br><strong>6.1. Hình thức thanh toán</strong><br>Tiền mặt. Chuyển khoản ngân hàng. Thanh toán trên website.<br><strong>6.2. Phụ phí có thể phát sinh</strong><br>Trả xe trễ giờ. Hư hỏng nhẹ hoặc nặng (tùy mức độ). Mất mũ bảo hiểm / phụ kiện. Trả sai điểm thuê.</p>
    <p><strong>7. Chính sách hủy & hoàn tiền</strong><br><strong>7.1. Hủy trước giờ thuê</strong><br>Hủy trước ≥ 2 giờ: hoàn 100%.<br>Hủy &lt; 2 giờ: hoàn 50%.<br>Đã nhận xe: không hoàn.<br><strong>7.2. Trường hợp từ chối thuê</strong><br>Đơn vị có quyền từ chối nếu: Người thuê không đủ giấy tờ. Có dấu hiệu sử dụng xe sai mục đích. Hành vi không hợp tác với nhân viên trạm.</p>
    <p><strong>8. Trách nhiệm & bồi thường</strong><br><strong>8.1. Trách nhiệm của người thuê</strong><br>Bồi thường 100% khi: Làm mất xe. Làm hỏng nặng do lỗi chủ quan. Chịu trách nhiệm pháp lý khi vi phạm luật giao thông.<br><strong>8.2. Trách nhiệm của đơn vị cho thuê</strong><br>Đảm bảo xe đủ tiêu chuẩn kỹ thuật, an toàn. Hỗ trợ khẩn cấp 24/7. Bảo mật thông tin khách hàng.</p>
    <p><strong>9. Cam kết bảo mật thông tin</strong><br>Thông tin người thuê được bảo vệ tuyệt đối. Không chia sẻ cho bên thứ ba trừ khi pháp luật yêu cầu.</p>
    <p><strong>10. Liên hệ hỗ trợ</strong><br>Hotline: 0915907623<br>Email: ngotruongtin0111@gmail.com<br>Website: https://carrental.com</p>
`;

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

function hasCheckedIn(record) {
    if (!record) return false;
    const status = (record.status || "").toUpperCase();
    if (["IN_PROGRESS", "WAITING_INSPECTION", "RETURNED", "COMPLETED"].includes(status)) return true;
    return Boolean(record.startTime);
}

function openContractModal(rentalId, afterAccept) {
    pendingContractRentalId = rentalId;
    contractAcceptedCallback = afterAccept || null;
    contractModalState.el?.classList.add("show");
}

function closeContractModal() {
    pendingContractRentalId = null;
    contractAcceptedCallback = null;
    contractModalState.el?.classList.remove("show");
}

async function acceptContract() {
    if (!pendingContractRentalId) return closeContractModal();
    try {
        const res = await fetch(`/api/rental/${pendingContractRentalId}/sign-contract`, { method: "POST" });
        const txt = await res.text();
        if (!res.ok) return alert(txt || "Không ký được hợp đồng");
        alert("Bạn đã chấp thuận hợp đồng. Có thể tiếp tục check-in nhận xe.");
        closeContractModal();
        if (typeof contractAcceptedCallback === "function") contractAcceptedCallback();
        loadHistory();
    } catch (e) {
        alert("Không ký được hợp đồng. Thử lại sau.");
    }
}

function startCheckinFlow(record) {
    if (!record) return;
    if (!record.contractSigned) {
        openContractModal(record.id, () => openCheckinModal(record));
        return;
    }
    openCheckinModal(record);
}

function openCheckinModal(record) {
    activeCheckinRecord = record;
    resetCheckinModal();
    checkinModalState.el?.classList.add("show");
}

function closeCheckinModal() {
    activeCheckinRecord = null;
    checkinModalState.el?.classList.remove("show");
}

function resetCheckinModal() {
    if (checkinModalState.notesInput) checkinModalState.notesInput.value = "";
    if (checkinModalState.photoInput) checkinModalState.photoInput.value = "";
    if (checkinModalState.preview) {
        checkinModalState.preview.innerHTML = "<span>Chưa có ảnh</span>";
    }
}

function handleCheckinPhotoChange(event) {
    const file = event?.target?.files?.[0];
    if (!file || !checkinModalState.preview) {
        if (checkinModalState.preview) checkinModalState.preview.innerHTML = "<span>Chưa có ảnh</span>";
        return;
    }

    const reader = new FileReader();
    reader.onload = (e) => {
        checkinModalState.preview.innerHTML = `<img src="${e.target?.result}" alt="Ảnh check-in">`;
    };
    reader.readAsDataURL(file);
}

async function submitCheckin() {
    if (!activeCheckinRecord) return;
    const photoFile = checkinModalState.photoInput?.files?.[0];
    if (!photoFile) {
        alert("Vui lòng chụp hoặc tải ảnh tình trạng xe trước khi check-in.");
        return;
    }

    const formData = new FormData();
    formData.append("photo", photoFile);
    formData.append("notes", checkinModalState.notesInput?.value || "");

    try {
        const res = await fetch(`/api/rental/${activeCheckinRecord.id}/check-in`, {
            method: "POST",
            body: formData,
        });
        const txt = await res.text();
        if (!res.ok) return alert(txt || "Check-in thất bại");
        alert("Đã check-in thành công. Ảnh được lưu làm bằng chứng tình trạng xe.");
        closeCheckinModal();
        loadHistory();
    } catch (e) {
        alert("Không check-in được. Thử lại sau.");
    }
}

function canContinuePayment(record) {
    if (!record) return false;
    if (isCancelled(record) || isCompleted(record)) return false;

    const statusUpper = (record.status || "").toUpperCase();
    const paymentStatus = (record.paymentStatus || "").toUpperCase();

    const hasPendingPayment =
        statusUpper === "PENDING_PAYMENT" ||
        paymentStatus === "PENDING" ||
        !record.paymentMethod;

    if (!hasPendingPayment) return false;

    if (!record.holdExpiresAt) return true;
    const holdExpiry = new Date(record.holdExpiresAt);
    return !isNaN(holdExpiry.getTime()) && holdExpiry > new Date();
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
    const checkedIn = hasCheckedIn(record);

    if (!disabled && !record.contractSigned) {
        const btn = document.createElement("button");
        btn.className = "action-button";
        btn.innerHTML = '<i class="fas fa-file-signature"></i> Ký hợp đồng điện tử';
        btn.onclick = () => openContractModal(record.id);
        actions.appendChild(btn);
    }

    if (!disabled && !checkedIn) {
        const btnCheckin = document.createElement("button");
        btnCheckin.className = "action-button";
        btnCheckin.innerHTML = '<i class="fas fa-check"></i> Check-in nhận xe';
        btnCheckin.onclick = () => startCheckinFlow(record);
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

    if (canContinuePayment(record)) {
        const paymentSection = document.createElement("div");
        paymentSection.className = "modal-section payment-action";
        paymentSection.innerHTML = `
            <div class="payment-callout">
                <div class="payment-text">
                    <h4>Chưa hoàn tất thanh toán</h4>
                    <p>${record.holdExpiresAt
                        ? `Giữ chỗ tới: ${formatDateTime(record.holdExpiresAt)}`
                        : "Bạn có thể chọn phương thức thanh toán để tiếp tục giữ xe."}</p>
                </div>
                <button type="button" class="btn-continue-payment">
                    <i class="fas fa-credit-card"></i> Thanh toán ngay
                </button>
            </div>
        `;

        paymentSection.querySelector(".btn-continue-payment")?.addEventListener("click", () => {
            window.location.href = `/thanhtoan?rentalId=${encodeURIComponent(record.id)}`;
        });

        rentalModal.body.appendChild(paymentSection);
    }

    rentalModal.el.classList.add("show");
}

function initContractModal() {
    contractModalState.el = document.getElementById("contract-modal");
    contractModalState.body = document.getElementById("contract-body");
    contractModalState.acceptBtn = document.getElementById("btn-contract-accept");

    if (contractModalState.body) contractModalState.body.innerHTML = CONTRACT_HTML;
    contractModalState.acceptBtn?.addEventListener("click", acceptContract);
    document.getElementById("btn-contract-cancel")?.addEventListener("click", closeContractModal);
    contractModalState.el?.querySelector(".dialog-overlay")?.addEventListener("click", closeContractModal);
    contractModalState.el?.querySelector(".dialog-close")?.addEventListener("click", closeContractModal);
}

function initCheckinModal() {
    checkinModalState.el = document.getElementById("checkin-modal");
    checkinModalState.notesInput = document.getElementById("checkin-notes");
    checkinModalState.photoInput = document.getElementById("checkin-photo");
    checkinModalState.preview = document.getElementById("checkin-preview");

    resetCheckinModal();
    checkinModalState.photoInput?.addEventListener("change", handleCheckinPhotoChange);
    document.getElementById("btn-checkin-confirm")?.addEventListener("click", submitCheckin);
    document.getElementById("btn-checkin-cancel")?.addEventListener("click", closeCheckinModal);
    checkinModalState.el?.querySelector(".dialog-overlay")?.addEventListener("click", closeCheckinModal);
    checkinModalState.el?.querySelector(".dialog-close")?.addEventListener("click", closeCheckinModal);
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
    initContractModal();
    initCheckinModal();

    document.querySelector(".btn-filter")?.addEventListener("click", filterHistory);
    document.getElementById("period-filter")?.addEventListener("change", filterHistory);
    document.getElementById("vehicle-type-filter")?.addEventListener("change", filterHistory);
    document.getElementById("status-filter")?.addEventListener("change", filterHistory);
});
