let historyData = [];
let latestCheckinPosition = null;
let latestReturnPosition = null;

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

const returnModalState = {
    el: null,
    notesInput: null,
    photoInput: null,
    preview: null,
};

const editModalState = {
    el: null,
    startInput: null,
    endInput: null,
    rentalId: null,
};

const extraFeeModalState = {
    el: null,
    amountEl: null,
    noteEl: null,
    qrEl: null,
    descEl: null,
};

let pendingContractRentalId = null;
let contractAcceptedCallback = null;
let activeCheckinRecord = null;
let activeReturnRecord = null;
let activeCheckinStation = null;
let activeReturnStation = null;

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

function formatCoords(lat, lon) {
    if (typeof lat !== "number" || typeof lon !== "number") return "";
    return `${lat.toFixed(5)}, ${lon.toFixed(5)}`;
}

function toImageDataUrl(base64) {
    if (!base64) return "";
    if (typeof base64 !== "string") return "";
    return base64.startsWith("data:") ? base64 : `data:image/jpeg;base64,${base64}`;
}

function formatInputDate(dateStr) {
    const parsed = parseDate(dateStr);
    if (!parsed) return "";
    const year = parsed.getFullYear();
    const month = `${parsed.getMonth() + 1}`.padStart(2, "0");
    const day = `${parsed.getDate()}`.padStart(2, "0");
    return `${year}-${month}-${day}`;
}

function formatMoney(value) {
    if (value === undefined || value === null || isNaN(value)) return "---";
    return `${Number(value).toLocaleString("vi-VN")} VNĐ`;
}

function formatPaymentMethod(method) {
    if (!method) return "---";
    const normalized = method.toUpperCase();
    const map = {
        "BANK_TRANSFER": "Chuyển khoản",
        "CASH": "Tiền mặt",
        "PAY_AT_STATION": "Thanh toán tại trạm",
        "TRANSFER": "Chuyển khoản",
        "CHUYENKHOAN": "Chuyển khoản",
        "TIENMAT": "Tiền mặt"
    };
    return map[normalized] || method;
}

function formatPaymentStatus(status, record) {
    if (!status) return "Chưa cập nhật";
    const normalized = status.toUpperCase();
    
    // Check if completed/returned and has no unpaid extra fees - should show "Hoàn tất"
    if (record) {
        const recordStatus = (record.status || "").toUpperCase();
        const extraAmount = Number(record.additionalFeeAmount || 0);
        const extraPaid = Number(record.additionalFeePaidAmount || 0);
        const hasUnpaidExtraFee = extraAmount > 0 && extraPaid < extraAmount;
        
        if (["COMPLETED", "RETURNED"].includes(recordStatus)) {
            if (hasUnpaidExtraFee) {
                return "Chờ thanh toán phí phát sinh";
            }
            return "Hoàn tất";
        }
    }
    
    // Status mappings for when record context is not available or record is not completed/returned
    const map = {
        "PENDING": "Chờ thanh toán",
        "PAID": "Đã thanh toán",
        "UNPAID": "Chưa thanh toán",
        "PAY_AT_STATION": "Thanh toán tại trạm",
        "BANK_TRANSFER": "Đã chuyển khoản",
        "DEPOSIT_PAID": "Đã đặt cọc 30%",
        "PENDING_EXTRA_FEE": "Chờ thanh toán phí phát sinh",
        "COMPLETED": "Hoàn tất",
        "CANCELLED": "Đã hủy",
        "EXPIRED": "Đã hết hạn",
        "CHUA_THANH_TOAN": "Chưa thanh toán",
        "PENDING_PAYMENT": "Chờ thanh toán"
    };
    
    return map[normalized] || status;
}

function haversineDistanceMeters(lat1, lon1, lat2, lon2) {
    const R = 6371000; // m
    const toRad = deg => (deg * Math.PI) / 180;
    const dLat = toRad(lat2 - lat1);
    const dLon = toRad(lon2 - lon1);
    const a = Math.sin(dLat / 2) ** 2 + Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLon / 2) ** 2;
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
}

function getCurrentPosition() {
    return new Promise((resolve, reject) => {
        if (!navigator.geolocation) return reject(new Error("Thiết bị không hỗ trợ định vị."));
        navigator.geolocation.getCurrentPosition(
            (pos) => resolve(pos.coords),
            (err) => reject(err),
            { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
        );
    });
}

function isWithinRentalWindow(record) {
    if (!record) return true;
    const start = parseDate(record.startDate || record.startTime);
    const end = parseDate(record.endDate || record.endTime);
    const now = new Date();

    if (start) {
        const startWindow = new Date(start);
        startWindow.setHours(0, 0, 0, 0);
        if (now < startWindow) return false;
    }

    if (end) {
        const endWindow = new Date(end);
        endWindow.setHours(23, 59, 59, 999);
        if (now > endWindow) return false;
    }

    return true;
}

async function ensureWithinStationRadius(station) {
    if (!station || typeof station.latitude !== "number" || typeof station.longitude !== "number") {
        throw new Error("Không lấy được vị trí trạm.");
    }
    const coords = await getCurrentPosition();
    const distance = haversineDistanceMeters(coords.latitude, coords.longitude, station.latitude, station.longitude);
    if (Number.isNaN(distance)) throw new Error("Không xác định được khoảng cách tới trạm.");
    if (distance > 50) {
        const rounded = Math.round(distance);
        throw new Error(`Vị trí nằm ngoài khu vực trạm hoặc sai trạm (cách ${rounded}m).`);
    }
    return { coords, distance };
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

function isExpiredRental(record) {
    if (!record) return false;
    const status = (record.status || "").toUpperCase();
    const paymentStatus = (record.paymentStatus || "").toUpperCase();
    return status === "EXPIRED" || paymentStatus === "NO_SHOW";
}

function canModifyReservation(record) {
    if (!record || isCancelled(record) || isCompleted(record)) return false;
    const status = (record.status || "").toUpperCase();
    if (["IN_PROGRESS", "WAITING_INSPECTION"].includes(status)) return false;
    const startDate = parseDate(record.startDate || record.startTime);
    if (startDate) {
        const startEndOfDay = new Date(startDate);
        startEndOfDay.setHours(23, 59, 59, 999);
        if (new Date() > startEndOfDay && hasCheckedIn(record)) return false;
    }
    return true;
}

function hasCheckedIn(record) {
    if (!record) return false;
    const status = (record.status || "").toUpperCase();
    if (["IN_PROGRESS", "WAITING_INSPECTION", "RETURNED", "COMPLETED"].includes(status)) return true;

    const hasPhoto = Boolean(record.checkinPhotoData);
    const actualStart = parseDate(record.checkinTime || record.actualStartTime);
    return hasPhoto || !!actualStart;
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

async function startCheckinFlow(record, station) {
    if (!record) return;
    if (!record.contractSigned) {
        alert("Vui lòng ký hợp đồng trước khi thực hiện check-in.");
        return;
    }
    if (!isWithinRentalWindow(record)) {
        alert("Chỉ được check-in trong đúng thời gian thuê đã đăng ký.");
        return;
    }
    if (!station || typeof station.latitude !== "number" || typeof station.longitude !== "number") {
        alert("Không lấy được vị trí trạm thuê. Vui lòng thử lại sau.");
        return;
    }

    try {
        const { coords } = await ensureWithinStationRadius(station);
        latestCheckinPosition = { latitude: coords.latitude, longitude: coords.longitude };
        activeCheckinStation = station;
        openCheckinModal(record);
    } catch (err) {
        console.error("Geo error", err);
        alert(err.message || "Không lấy được vị trí hiện tại. Vui lòng bật GPS và thử lại.");
    }
}

function openCheckinModal(record) {
    activeCheckinRecord = record;
    resetCheckinModal();
    checkinModalState.el?.classList.add("show");
}

function closeCheckinModal() {
    activeCheckinRecord = null;
    activeCheckinStation = null;
    checkinModalState.el?.classList.remove("show");
}

function resetCheckinModal() {
    latestCheckinPosition = null;
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

    try {
        const { coords } = await ensureWithinStationRadius(activeCheckinStation);
        latestCheckinPosition = { latitude: coords.latitude, longitude: coords.longitude };
    } catch (err) {
        alert(err.message || "Không lấy được vị trí hiện tại để check-in.");
        return;
    }

    const formData = new FormData();
    formData.append("photo", photoFile);
    formData.append("notes", checkinModalState.notesInput?.value || "");
    formData.append("latitude", latestCheckinPosition.latitude);
    formData.append("longitude", latestCheckinPosition.longitude);

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

function openReturnModal(record) {
    activeReturnRecord = record;
    resetReturnModal();
    returnModalState.el?.classList.add("show");
}

function closeReturnModal() {
    activeReturnRecord = null;
    activeReturnStation = null;
    returnModalState.el?.classList.remove("show");
}

function resetReturnModal() {
    latestReturnPosition = null;
    if (returnModalState.notesInput) returnModalState.notesInput.value = "";
    if (returnModalState.photoInput) returnModalState.photoInput.value = "";
    if (returnModalState.preview) returnModalState.preview.innerHTML = "<span>Chưa có ảnh</span>";
}

function handleReturnPhotoChange(event) {
    const file = event?.target?.files?.[0];
    if (!file || !returnModalState.preview) {
        if (returnModalState.preview) returnModalState.preview.innerHTML = "<span>Chưa có ảnh</span>";
        return;
    }

    const reader = new FileReader();
    reader.onload = (e) => {
        returnModalState.preview.innerHTML = `<img src="${e.target?.result}" alt="Ảnh trả xe">`;
    };
    reader.readAsDataURL(file);
}

async function startReturnFlow(record, station) {
    if (!record) return;
    if (!hasCheckedIn(record)) {
        alert("Bạn cần check-in nhận xe trước khi trả.");
        return;
    }
    if (!station || typeof station.latitude !== "number" || typeof station.longitude !== "number") {
        alert("Không lấy được vị trí trạm trả. Vui lòng thử lại sau.");
        return;
    }

    try {
        const { coords } = await ensureWithinStationRadius(station);
        latestReturnPosition = { latitude: coords.latitude, longitude: coords.longitude };
        activeReturnStation = station;
        openReturnModal(record);
    } catch (err) {
        alert(err.message || "Không lấy được vị trí hiện tại để trả xe.");
    }
}

async function submitReturn() {
    if (!activeReturnRecord) return;
    const photoFile = returnModalState.photoInput?.files?.[0];
    if (!photoFile) {
        alert("Vui lòng chụp hoặc tải ảnh tình trạng xe trước khi trả.");
        return;
    }

    try {
        const { coords } = await ensureWithinStationRadius(activeReturnStation);
        latestReturnPosition = { latitude: coords.latitude, longitude: coords.longitude };
    } catch (err) {
        alert(err.message || "Không lấy được vị trí hiện tại để trả xe.");
        return;
    }

    const formData = new FormData();
    formData.append("photo", photoFile);
    formData.append("notes", returnModalState.notesInput?.value || "");
    formData.append("latitude", latestReturnPosition.latitude);
    formData.append("longitude", latestReturnPosition.longitude);

    try {
        const res = await fetch(`/api/rental/${activeReturnRecord.id}/return`, {
            method: "POST",
            body: formData,
        });
        const txt = await res.text();
        if (!res.ok) return alert(txt || "Không gửi được yêu cầu trả xe");
        alert("Đã yêu cầu trả xe và lưu ảnh tình trạng.");
        closeReturnModal();
        loadHistory();
    } catch (e) {
        alert("Không gửi được yêu cầu trả xe. Thử lại sau.");
    }
}

function openEditModal(record) {
    if (!record || !editModalState.el) return;
    editModalState.rentalId = record.id;
    const minDate = formatInputDate(new Date());
    if (editModalState.startInput) {
        editModalState.startInput.min = minDate;
        editModalState.startInput.value = formatInputDate(record.startDate || record.startTime);
    }
    if (editModalState.endInput) {
        editModalState.endInput.min = minDate;
        editModalState.endInput.value = formatInputDate(record.endDate || record.endTime || record.startDate);
    }
    editModalState.el.classList.add("show");
}

function closeEditModal() {
    editModalState.rentalId = null;
    if (editModalState.startInput) editModalState.startInput.value = "";
    if (editModalState.endInput) editModalState.endInput.value = "";
    editModalState.el?.classList.remove("show");
}

function updateHistoryRecord(rentalId, updates) {
    if (!rentalId) return;
    historyData = historyData.map(item => {
        if (item.record?.id === rentalId) {
            return { ...item, record: { ...item.record, ...updates } };
        }
        return item;
    });
    renderHistoryList(historyData);
}

function removeHistoryRecord(rentalId) {
    if (!rentalId) return;
    historyData = historyData.filter(item => item.record?.id !== rentalId);
    renderHistoryList(historyData);
}

async function submitEditDates() {
    if (!editModalState.rentalId) return;
    const startDate = editModalState.startInput?.value;
    const endDate = editModalState.endInput?.value;

    if (!startDate || !endDate) {
        alert("Vui lòng chọn đủ ngày bắt đầu và kết thúc.");
        return;
    }

    if (new Date(startDate) < new Date(new Date().toISOString().split("T")[0])) {
        alert("Ngày bắt đầu phải từ hôm nay trở đi.");
        return;
    }
    if (new Date(endDate) < new Date(startDate)) {
        alert("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu.");
        return;
    }

    try {
        const res = await fetch(`/api/rental/${editModalState.rentalId}/dates`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ startDate, endDate })
        });
        const data = await res.text();
        if (!res.ok) {
            alert(data || "Không chỉnh sửa được ngày thuê.");
            return;
        }
        const parsed = data ? JSON.parse(data) : {};
        updateHistoryRecord(editModalState.rentalId, parsed);
        closeEditModal();
        alert("Đã cập nhật ngày thuê.");
    } catch (err) {
        alert(err.message || "Không chỉnh sửa được ngày thuê. Vui lòng thử lại.");
    }
}

async function cancelRental(record) {
    if (!record?.id) return;
    if (!confirm("Bạn có chắc muốn hủy chuyến thuê này?")) return;
    try {
        const res = await fetch(`/api/rental/${record.id}/cancel`, { method: "POST" });
        const data = await res.text();
        if (!res.ok) {
            alert(data || "Không hủy được chuyến thuê.");
            return;
        }
        removeHistoryRecord(record.id);
        alert("Đã hủy đơn thuê! Nếu đã đặt cọc hoặc chuyển khoản thì gửi yêu cầu hỗ trợ trong hồ sơ cá nhân để được hoàn tiền.");
    } catch (err) {
        alert(err.message || "Không hủy được chuyến thuê. Thử lại sau.");
    }
}

function hasDepositRequirement(record) {
    if (!record) return false;
    const depositRequired = Number(record.depositRequiredAmount || 0);
    const depositPaid = Number(record.depositPaidAmount || 0);
    return depositRequired > 0 && depositPaid < depositRequired;
}

function isOfflinePaymentMethod(method) {
    const normalized = (method || "").toUpperCase();
    return ["CASH", "PAY_AT_STATION", "BANK_TRANSFER", "TRANSFER", "CHUYENKHOAN", "TIENMAT"]
        .some(flag => normalized.includes(flag));
}

function canContinuePayment(record) {
    if (!record) return false;
    if (isCancelled(record) || isCompleted(record)) return false;

    const statusUpper = (record.status || "").toUpperCase();
    const paymentStatus = (record.paymentStatus || "").toUpperCase();
    const offlinePayment = isOfflinePaymentMethod(record.paymentMethod);
    const depositPending = hasDepositRequirement(record);

    const hasPendingPayment =
        statusUpper === "PENDING_PAYMENT" ||
        paymentStatus === "PENDING" ||
        paymentStatus === "UNPAID" ||
        paymentStatus === "CHUA_THANH_TOAN" ||
        !record.paymentMethod;

    if (offlinePayment && !depositPending) return false;
    if (!hasPendingPayment) return false;

    if (!record.holdExpiresAt) return true;
    const holdExpiry = new Date(record.holdExpiresAt);
    return !isNaN(holdExpiry.getTime()) && holdExpiry > new Date();
}

function hasOutstandingUpfrontPayment(record) {
    if (!record) return false;
    const depositPending = hasDepositRequirement(record);

    return depositPending || canContinuePayment(record);
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
    const paymentStatusUpper = (record.paymentStatus || "").toUpperCase();
    const paymentMethod = (record.paymentMethod || "").toUpperCase();
    const unpaidStatus = ["PENDING", "UNPAID", "CHUA_THANH_TOAN", "PENDING_PAYMENT"].includes(paymentStatusUpper);

    // Kiểm tra các trạng thái cần highlight
    const completedOrReturned = ["COMPLETED", "RETURNED"].includes(statusUpper);
    let highlightUnpaid = statusUpper === "PENDING_PAYMENT" || unpaidStatus;
    const isCurrentlyRenting = statusUpper === "IN_PROGRESS"; // Đang thuê xe
    const isWaitingInspection = statusUpper === "WAITING_INSPECTION"; // Chờ kiểm tra

    // Không highlight nếu đã hoàn thành và không có phí phát sinh chưa thanh toán
    // Kiểm tra phí phát sinh
    const extraAmount = Number(record.additionalFeeAmount || 0);
    const extraPaid = Number(record.additionalFeePaidAmount || 0);
    const hasUnpaidExtraFee = extraAmount > 0 && extraPaid < extraAmount;

    // Nếu đã hoàn thành và không còn phí phát sinh chưa thanh toán, không cần warning
    if (completedOrReturned && !hasUnpaidExtraFee) {
        highlightUnpaid = false;
    }

    // Thêm warning cho status badge
    if ((highlightUnpaid && !completedOrReturned) || isWaitingInspection || (completedOrReturned && hasUnpaidExtraFee)) {
        statusBadge.classList.add("warning");
    }
    actions.appendChild(statusBadge);

    const disabled = isCancelled(record) || isCompleted(record);
    const checkedIn = hasCheckedIn(record);
    const withinWindow = isWithinRentalWindow(record);
    const modifiable = canModifyReservation(record);
    const pendingPayment = hasOutstandingUpfrontPayment(record);

    // Highlight khi: đang thuê HOẶC chờ kiểm tra HOẶC còn khoản chưa thanh toán (nhưng không nếu đã hoàn thành và trả phí xong)
    let highlightUnpaidItem = isCurrentlyRenting || isWaitingInspection || pendingPayment || highlightUnpaid || hasUnpaidExtraFee;
    
    // Không highlight nếu đã hoàn thành và không có phí phát sinh chưa thanh toán
    if (completedOrReturned && !hasUnpaidExtraFee) {
        highlightUnpaidItem = false;
    }

    if (highlightUnpaidItem) {
        container.classList.add("unpaid-highlight");
    }

    if (pendingPayment) {
        const note = document.createElement("span");
        note.className = "status-badge warning";
        note.innerText = "Vui lòng thanh toán cọc/chuyển khoản trước khi tiếp tục";
        actions.appendChild(note);
    }

    if (!disabled && !record.contractSigned) {
        const btn = document.createElement("button");
        btn.className = "action-button";
        btn.innerHTML = '<i class="fas fa-file-signature"></i> Ký hợp đồng điện tử';
        btn.disabled = pendingPayment;
        if (pendingPayment) {
            btn.title = "Thanh toán cọc/chuyển khoản trước khi ký hợp đồng";
        }
        btn.onclick = () => {
            if (pendingPayment) return;
            openContractModal(record.id);
        };
        actions.appendChild(btn);
    }

    if (!disabled && !checkedIn) {
        const btnCheckin = document.createElement("button");
        btnCheckin.className = "action-button";
        btnCheckin.innerHTML = '<i class="fas fa-check"></i> Check-in nhận xe';
        btnCheckin.disabled = !record.contractSigned || !withinWindow;
        if (pendingPayment && !record.contractSigned) {
            btnCheckin.title = "Ký hợp đồng điện tử trước khi check-in";
        } else if (!record.contractSigned) {
            btnCheckin.title = "Ký hợp đồng điện tử trước khi check-in";
        } else if (!withinWindow) {
            btnCheckin.title = "Chỉ check-in trong thời gian thuê đã đăng ký";
        }
        btnCheckin.onclick = () => startCheckinFlow(record, station);
        actions.appendChild(btnCheckin);
    }

    if (!disabled && statusUpper !== "WAITING_INSPECTION") {
        const btnReturn = document.createElement("button");
        btnReturn.className = "action-button secondary";
        btnReturn.innerHTML = '<i class="fas fa-undo"></i> Yêu cầu trả xe';
        btnReturn.disabled = pendingPayment;
        if (pendingPayment) {
            btnReturn.title = "Thanh toán cọc/chuyển khoản trước khi yêu cầu trả xe";
        }
        btnReturn.onclick = () => startReturnFlow(record, station);
        actions.appendChild(btnReturn);
    } else if (statusUpper === "WAITING_INSPECTION") {
        const note = document.createElement("span");
        note.className = "status-badge warning";
        note.innerText = "Đã gửi yêu cầu trả xe";
        actions.appendChild(note);
    }

    if (modifiable) {
        const btnEdit = document.createElement("button");
        btnEdit.className = "action-button secondary";
        btnEdit.innerHTML = '<i class="fas fa-calendar-day"></i> Chỉnh sửa ngày thuê';
        btnEdit.onclick = () => openEditModal(record);
        actions.appendChild(btnEdit);

        const btnCancel = document.createElement("button");
        btnCancel.className = "action-button secondary";
        btnCancel.innerHTML = '<i class="fas fa-times"></i> Hủy đơn';
        btnCancel.onclick = () => cancelRental(record);
        actions.appendChild(btnCancel);
    }

    container.appendChild(actions);

    // Add notice for expired rentals
    if (isExpiredRental(record) && record.additionalFeeNote) {
        const notice = document.createElement("div");
        
        // Phân biệt tiền mặt vs chuyển khoản
        const isCash = record.paymentMethod && record.paymentMethod.toLowerCase() === "cash";
        const bgColor = isCash ? "#ffebee" : "#fff3e0";
        const borderColor = isCash ? "#c62828" : "#f57c00";
        
        notice.style.cssText = `
            background: linear-gradient(135deg, ${bgColor} 0%, #fafafa 100%);
            border-left: 5px solid ${borderColor};
            padding: 20px;
            margin: 16px 0;
            border-radius: 12px;
            box-shadow: 0 4px 16px rgba(0,0,0,0.1);
        `;
        
        notice.innerHTML = `
            <div style="display: flex; gap: 16px;">
                <i class="fas fa-exclamation-circle" style="color: ${borderColor}; font-size: 36px; margin-top: 4px;"></i>
                <div style="flex: 1;">
                    <h4 style="color: ${borderColor}; margin: 0 0 16px 0; font-size: 20px; font-weight: 700;">
                        ${isCash ? '❌ Không hoàn tiền đặt cọc' : '⚠️ Đơn đã hết hạn'}
                    </h4>
                    <div style="background: white; padding: 16px; border-radius: 8px; margin-bottom: 16px; line-height: 1.8; white-space: pre-line; font-size: 14px; color: #424242;">
                        ${record.additionalFeeNote}
                    </div>
                    ${!isCash ? `
                    <a href="/profile#support" 
                       style="display: inline-flex; align-items: center; gap: 10px; 
                              padding: 12px 24px; background: linear-gradient(135deg, #ff6b6b, #d32f2f); 
                              color: white; text-decoration: none; border-radius: 10px; 
                              font-weight: 700; font-size: 15px; box-shadow: 0 4px 12px rgba(211,47,47,0.3);
                              transition: transform 0.2s;">
                        <i class="fas fa-headset"></i>
                        Yêu cầu hoàn tiền
                    </a>
                    ` : `
                    <div style="padding: 12px; background: #fff3e0; border-radius: 8px; border-left: 4px solid #f57c00;">
                        <i class="fas fa-info-circle" style="color: #f57c00;"></i>
                        <strong style="color: #e65100;">Lưu ý:</strong> 
                        Tiền đặt cọc không được hoàn lại theo chính sách thanh toán tiền mặt.
                    </div>
                    `}
                </div>
            </div>
        `;
        
        container.appendChild(notice);
    }

    // Add review button for completed bookings
    if (isCompleted(record) && typeof addReviewButton === 'function') {
        addReviewButton(container, record, vehicle, station);
    }

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

function getSortTimestamp(item) {
    const record = item?.record || item || {};
    const timestamps = [
        record.endTime ?? item?.endTime,
        record.startTime ?? item?.startTime,
        record.paidAt ?? item?.paidAt,
        record.depositPaidAt ?? item?.depositPaidAt,
        record.additionalFeePaidAt ?? item?.additionalFeePaidAt,
        record.holdExpiresAt ?? item?.holdExpiresAt,
        record.createdAt ?? item?.createdAt,
        record.startDate ?? item?.startDate
    ]
        .map(parseDate)
        .map(d => (d ? d.getTime() : null))
        .filter(v => Number.isFinite(v));

    if (timestamps.length) return Math.max(...timestamps);

    const objectIdTimestamp = [record.id, item?.id]
        .map(parseObjectIdTimestamp)
        .find(num => Number.isFinite(num));

    if (Number.isFinite(objectIdTimestamp)) return objectIdTimestamp;

    const numericId = [record.id, item?.id]
        .map(parseNumericId)
        .find(num => Number.isFinite(num));

    return Number.isFinite(numericId) ? numericId : 0;
}

function getSortKey(item) {
    const record = item?.record || item || {};
    const primaryValue = getSortTimestamp(item);
    const primary = Number.isFinite(primaryValue) ? primaryValue : 0;
    const created = parseDate(record.createdAt ?? item?.createdAt)?.getTime() ?? 0;
    const objectIdTs = parseObjectIdTimestamp(record.id ?? item?.id) ?? 0;
    const numericId = parseNumericId(record.id ?? item?.id) ?? 0;
    return { primary, created, objectIdTs, numericId };
}

function compareHistoryItems(a, b) {
    const ka = getSortKey(a);
    const kb = getSortKey(b);

    return (kb.primary - ka.primary)
        || (kb.created - ka.created)
        || (kb.objectIdTs - ka.objectIdTs)
        || (kb.numericId - ka.numericId);
}

function renderHistoryList(list) {
    const listEl = document.getElementById("history-list");
    if (!list || !list.length) {
        listEl.innerHTML = "<p>Chưa có chuyến thuê nào.</p>";
        updateAnalyticsFromHistory([]);
        return;
    }

    listEl.innerHTML = "";
    const sortedList = [...list].sort(compareHistoryItems);
    sortedList.forEach(item => listEl.appendChild(renderHistoryItem(item)));
    updateAnalyticsFromHistory(sortedList);
}

function parseDate(input) {
    if (!input) return null;
    const dt = new Date(input);
    return isNaN(dt.getTime()) ? null : dt;
}

function parseObjectIdTimestamp(value) {
    if (!value || typeof value !== "string") return null;
    const trimmed = value.trim();
    if (trimmed.length >= 8 && /^[a-fA-F0-9]+$/.test(trimmed)) {
        const seconds = parseInt(trimmed.substring(0, 8), 16);
        return Number.isFinite(seconds) ? seconds * 1000 : null;
    }

    const digits = trimmed.replace(/[^0-9]/g, "");
    if (digits) {
        const asNumber = Number(digits);
        return Number.isFinite(asNumber) ? asNumber : null;
    }

    return null;
}

function parseObjectIdTimestamp(value) {
    if (!value || typeof value !== "string") return null;
    const trimmed = value.trim();
    if (trimmed.length >= 8 && /^[a-fA-F0-9]+$/.test(trimmed)) {
        const seconds = parseInt(trimmed.substring(0, 8), 16);
        return Number.isFinite(seconds) ? seconds * 1000 : null;
    }

    const digits = trimmed.replace(/[^0-9]/g, "");
    if (digits) {
        const asNumber = Number(digits);
        return Number.isFinite(asNumber) ? asNumber : null;
    }

    return null;
}

function parseObjectIdTimestamp(value) {
    if (!value || typeof value !== "string") return null;
    const trimmed = value.trim();
    if (trimmed.length >= 8 && /^[a-fA-F0-9]+$/.test(trimmed)) {
        const seconds = parseInt(trimmed.substring(0, 8), 16);
        return Number.isFinite(seconds) ? seconds * 1000 : null;
    }

    const digits = trimmed.replace(/[^0-9]/g, "");
    if (digits) {
        const asNumber = Number(digits);
        return Number.isFinite(asNumber) ? asNumber : null;
    }

    return null;
}

function parseObjectIdTimestamp(value) {
    if (!value || typeof value !== "string") return null;
    const trimmed = value.trim();
    if (trimmed.length >= 8 && /^[a-fA-F0-9]+$/.test(trimmed)) {
        const seconds = parseInt(trimmed.substring(0, 8), 16);
        return Number.isFinite(seconds) ? seconds * 1000 : null;
    }

    const digits = trimmed.replace(/[^0-9]/g, "");
    if (digits) {
        const asNumber = Number(digits);
        return Number.isFinite(asNumber) ? asNumber : null;
    }

    return null;
}

function parseNumericId(value) {
    if (value === undefined || value === null) return null;
    if (typeof value === "number" && Number.isFinite(value)) return value;
    const digits = String(value).replace(/[^0-9]/g, "");
    if (!digits) return null;
    const parsed = Number(digits);
    return Number.isFinite(parsed) ? parsed : null;
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

    const paymentStatusUpper = (record.paymentStatus || "").toUpperCase();
    const recordStatus = (record.status || "").toUpperCase();
    const extraAmount = Number(record.additionalFeeAmount || 0);
    const extraPaid = Number(record.additionalFeePaidAmount || 0);
    const hasUnpaidExtraFee = extraAmount > 0 && extraPaid < extraAmount;
    const completedOrReturned = ["COMPLETED", "RETURNED"].includes(recordStatus);
    
    // Determine payment warning - don't show warning if completed/returned without extra fees
    let paymentWarning = ["PENDING", "UNPAID", "CHUA_THANH_TOAN", "PENDING_PAYMENT"].includes(paymentStatusUpper) 
        || hasOutstandingUpfrontPayment(record);
    
    // If completed and no unpaid extra fees, no warning
    if (completedOrReturned && !hasUnpaidExtraFee) {
        paymentWarning = false;
    }
    // If completed but has unpaid extra fee, show warning
    if (completedOrReturned && hasUnpaidExtraFee) {
        paymentWarning = true;
    }

    if (record.paymentStatus) {
        badges.push({ 
            text: `Thanh toán: ${formatPaymentStatus(record.paymentStatus, record)}`, 
            className: paymentWarning ? "warning" : "" 
        });
    }
    if (record.paymentMethod) {
        badges.push({ 
            text: `PTTT: ${formatPaymentMethod(record.paymentMethod)}`, 
            className: "" 
        });
    }

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

function buildPhotoCard(label, base64, timestamp, latitude, longitude, notes) {
    const hasImage = Boolean(base64);
    const meta = [];
    const formattedTime = formatDateTime(timestamp);
    const coords = formatCoords(latitude, longitude);

    if (formattedTime) meta.push(`<span><i class="fas fa-clock"></i> ${formattedTime}</span>`);
    if (coords) meta.push(`<span><i class="fas fa-location-dot"></i> ${coords}</span>`);
    if (notes) meta.push(`<span><i class="fas fa-note-sticky"></i> ${notes}</span>`);

    return `
        <div class="photo-card">
            <div class="photo-card__header">
                <strong>${label}</strong>
            </div>
            <div class="photo-card__body">
                ${hasImage
        ? `<img src="${toImageDataUrl(base64)}" alt="${label}" loading="lazy">`
        : `<div class="photo-placeholder">Chưa có ảnh ${label.toLowerCase()}</div>`}
            </div>
            ${meta.length ? `<div class="photo-card__meta">${meta.join("<br>")}</div>` : ""}
        </div>
    `;
}

function buildPhotoSection(record) {
    if (!record) return "";
    const photos = [
        buildPhotoCard("Check-in", record.checkinPhotoData, record.checkinTime || record.startTime, record.checkinLatitude, record.checkinLongitude, record.checkinNotes),
        buildPhotoCard("Trả xe", record.returnPhotoData, record.endTime, record.returnLatitude, record.returnLongitude, record.returnNotes),
    ];

    if (!photos.some(Boolean)) return "";
    return `<div class="modal-section"><h4>Ảnh check-in / trả xe</h4><div class="photo-grid">${photos.join("")}</div></div>`;
}

async function startExtraFeePayment(record) {
    if (!record || !record.id) return;
    try {
        const res = await fetch(`/api/payment/extra-fee/create-order`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ rentalId: record.id })
        });

        if (!res.ok) {
            const msg = await res.text();
            alert(msg || "Không thể tạo QR cho phí phát sinh");
            return;
        }

        const payload = await res.json();
        openExtraFeeModal(payload);
    } catch (err) {
        console.error("Lỗi tạo QR phí phát sinh", err);
        alert("Không thể tạo QR cho phí phát sinh, vui lòng thử lại.");
    }
}

function openRentalModal(item) {
    if (!rentalModal.el || !rentalModal.body || !rentalModal.title) return;
    const record = item.record || {};
    const vehicle = item.vehicle || {};
    const station = item.station || {};

    const extraAmount = Number(record.additionalFeeAmount ?? item.additionalFeeAmount ?? record.damageFee ?? 0);
    const extraPaid = Number(record.additionalFeePaidAmount ?? item.additionalFeePaidAmount ?? 0);
    const extraOutstanding = Math.max(0, extraAmount - extraPaid);

    rentalModal.title.textContent = vehicle.brand ? `${vehicle.brand} (${vehicle.plate || ""})` : `Chuyến #${record.id}`;
    renderBadges(item);

    // Kiểm tra nếu đã check-in → hiển thị cả giờ
    const hasCheckedInRecord = record.checkinTime || record.checkinPhotoData;
    const startDisplay = hasCheckedInRecord 
        ? formatDateTime(record.startTime) 
        : formatDate(record.startDate || record.startTime);
    const endDisplay = record.endTime 
        ? (hasCheckedInRecord ? formatDateTime(record.endTime) : formatDate(record.endDate || record.endTime))
        : "Chưa trả";

    const rentalRows = [
        { label: "Mã chuyến", value: `#${record.id || ""}` },
        { label: "Thời gian thuê", value: `${startDisplay} - ${endDisplay}` },
        { label: "Số ngày", value: record.rentalDays ? `${record.rentalDays} ngày` : "---" },
        { label: "Tổng phí", value: formatMoney(record.total) },
        { label: "Trạng thái", value: item.displayStatus || record.status || "" },
        { label: "Thanh toán", value: formatPaymentStatus(record.paymentStatus, record) },
        { label: "PT thanh toán", value: formatPaymentMethod(record.paymentMethod) },
        { label: "Giữ chỗ tới", value: record.holdExpiresAt ? formatDateTime(record.holdExpiresAt) : "---" },
        extraAmount > 0
            ? { label: "Chi phí phát sinh", value: `${formatMoney(extraAmount)}${extraOutstanding > 0 ? ` (còn ${formatMoney(extraOutstanding)})` : ""}` }
            : null,
    ];

    const sanitizedRentalRows = rentalRows.filter(Boolean);

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
    if (record.additionalFeeNote && record.additionalFeeNote !== record.returnNotes) {
        notes.push(`<div class="note-block"><strong>Ghi chú phí phát sinh:</strong><br>${record.additionalFeeNote}</div>`);
    }

    const photoSection = buildPhotoSection(record);

    rentalModal.body.innerHTML = [
        buildDetailSection("Thông tin chuyến", sanitizedRentalRows),
        buildDetailSection("Xe & chi phí", vehicleRows),
        buildDetailSection("Trạm thuê", stationRows),
        photoSection,
        notes.length ? `<div class="modal-section">${notes.join("")}</div>` : "",
    ].join("");

    if (canContinuePayment(record)) {
        const paymentSection = document.createElement("div");
        paymentSection.className = "modal-section payment-action";
        paymentSection.innerHTML = `
            <div class="payment-callout">
                <div class="payment-text">
                    <h4>Đang giữ chỗ, cần thanh toán</h4>
                    <p>${record.holdExpiresAt
                        ? `Vui lòng thanh toán trước ${formatDateTime(record.holdExpiresAt)} để giữ xe.`
                        : "Thanh toán để tiếp tục quá trình thuê và giữ chỗ của bạn."}</p>
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

    if (extraOutstanding > 0) {
        const feeSection = document.createElement("div");
        feeSection.className = "modal-section payment-action";
        const note = record.additionalFeeNote || record.returnNotes || "Phí phát sinh do nhân viên xác nhận.";
        feeSection.innerHTML = `
            <div class="payment-callout fee-callout">
                <div class="payment-text">
                    <h4>Chi phí phát sinh</h4>
                    <p>${note}</p>
                    <p><strong>Còn phải thanh toán:</strong> ${formatMoney(extraOutstanding)}</p>
                </div>
                <button type="button" class="btn-continue-payment btn-extra-fee">
                    <i class="fas fa-qrcode"></i> Thanh toán phí phát sinh
                </button>
            </div>
        `;

        feeSection.querySelector(".btn-extra-fee")?.addEventListener("click", () => startExtraFeePayment(record));
        rentalModal.body.appendChild(feeSection);
    }

    // Add notice for expired rentals in modal
    if (isExpiredRental(record) && record.additionalFeeNote) {
        const section = document.createElement("div");
        
        // Phân biệt tiền mặt vs chuyển khoản
        const isCash = record.paymentMethod && record.paymentMethod.toLowerCase() === "cash";
        const bgColor = isCash ? "#ffebee" : "#fff8e1";
        const borderColor = isCash ? "#c62828" : "#ff9800";
        const iconColor = isCash ? "#c62828" : "#ff6f00";
        
        section.innerHTML = `
            <div style="background: linear-gradient(135deg, ${bgColor}, ${isCash ? '#fce4ec' : '#ffecb3'}); border: 3px solid ${borderColor}; padding: 24px; border-radius: 12px; text-align: center;">
                <i class="fas ${isCash ? 'fa-times-circle' : 'fa-clock'}" style="font-size: 56px; color: ${iconColor}; margin-bottom: 16px;"></i>
                <h3 style="color: ${isCash ? '#b71c1c' : '#e65100'}; margin: 0 0 16px 0; font-size: 22px;">
                    ${isCash ? '❌ Không hoàn tiền đặt cọc' : 'Đơn đã hết hạn giữ chỗ'}
                </h3>
                <p style="margin: 0 0 20px 0; white-space: pre-line; line-height: 1.9; text-align: left; background: white; padding: 16px; border-radius: 8px;">${record.additionalFeeNote}</p>
                ${!isCash ? `
                <a href="/profile#support" style="display: inline-flex; align-items: center; gap: 10px; padding: 14px 28px; background: linear-gradient(135deg, #ff6b6b, #d32f2f); color: white; text-decoration: none; border-radius: 10px; font-weight: 700; font-size: 16px;">
                    <i class="fas fa-headset"></i> Liên hệ hỗ trợ để hoàn tiền
                </a>
                ` : `
                <div style="padding: 12px; background: #fff3e0; border-radius: 8px; border-left: 4px solid #f57c00; text-align: left; margin-top: 16px;">
                    <i class="fas fa-info-circle" style="color: #f57c00;"></i>
                    <strong style="color: #e65100;">Lưu ý:</strong> 
                    Tiền đặt cọc không được hoàn lại theo chính sách thanh toán tiền mặt.
                </div>
                `}
            </div>
        `;
        rentalModal.body.appendChild(section);
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

function initReturnModal() {
    returnModalState.el = document.getElementById("return-modal");
    returnModalState.notesInput = document.getElementById("return-notes");
    returnModalState.photoInput = document.getElementById("return-photo");
    returnModalState.preview = document.getElementById("return-preview");

    resetReturnModal();
    returnModalState.photoInput?.addEventListener("change", handleReturnPhotoChange);
    document.getElementById("btn-return-confirm")?.addEventListener("click", submitReturn);
    document.getElementById("btn-return-cancel")?.addEventListener("click", closeReturnModal);
    returnModalState.el?.querySelector(".dialog-overlay")?.addEventListener("click", closeReturnModal);
    returnModalState.el?.querySelector(".dialog-close")?.addEventListener("click", closeReturnModal);
}

function initExtraFeeModal() {
    extraFeeModalState.el = document.getElementById("extra-fee-modal");
    extraFeeModalState.amountEl = document.getElementById("extra-fee-amount");
    extraFeeModalState.noteEl = document.getElementById("extra-fee-note");
    extraFeeModalState.qrEl = document.getElementById("extra-fee-qr");
    extraFeeModalState.descEl = document.getElementById("extra-fee-desc");

    document.getElementById("btn-extra-fee-close")?.addEventListener("click", closeExtraFeeModal);
    extraFeeModalState.el?.querySelector(".dialog-overlay")?.addEventListener("click", closeExtraFeeModal);
    extraFeeModalState.el?.querySelector(".dialog-close")?.addEventListener("click", closeExtraFeeModal);
}

function initEditModal() {
    editModalState.el = document.getElementById("edit-dates-modal");
    editModalState.startInput = document.getElementById("edit-start-date");
    editModalState.endInput = document.getElementById("edit-end-date");

    const minDate = formatInputDate(new Date());
    if (editModalState.startInput) editModalState.startInput.min = minDate;
    if (editModalState.endInput) editModalState.endInput.min = minDate;

    document.getElementById("btn-edit-dates-confirm")?.addEventListener("click", submitEditDates);
    document.getElementById("btn-edit-dates-cancel")?.addEventListener("click", closeEditModal);
    editModalState.el?.querySelector(".dialog-overlay")?.addEventListener("click", closeEditModal);
    editModalState.el?.querySelector(".dialog-close")?.addEventListener("click", closeEditModal);
}

function openExtraFeeModal(payload) {
    if (!extraFeeModalState.el) return;
    const amount = payload?.amount ?? payload?.outstanding ?? 0;
    if (extraFeeModalState.amountEl) extraFeeModalState.amountEl.textContent = formatMoney(amount);
    if (extraFeeModalState.noteEl) extraFeeModalState.noteEl.textContent = payload?.note || "Phí phát sinh do nhân viên xác nhận.";
    if (extraFeeModalState.qrEl) extraFeeModalState.qrEl.src = payload?.qrBase64 || payload?.qrUrl || "";
    if (extraFeeModalState.descEl) extraFeeModalState.descEl.textContent = payload?.description || payload?.rentalId || "";
    extraFeeModalState.el.classList.add("show");
}

function closeExtraFeeModal() {
    extraFeeModalState.el?.classList.remove("show");
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
    initReturnModal();
    initEditModal();
    initExtraFeeModal();

    document.querySelector(".btn-filter")?.addEventListener("click", filterHistory);
    document.getElementById("period-filter")?.addEventListener("change", filterHistory);
    document.getElementById("vehicle-type-filter")?.addEventListener("change", filterHistory);
    document.getElementById("status-filter")?.addEventListener("change", filterHistory);
});
