// ================================
// LẤY rentalId
// ================================
function getRentalId() {
    const params = new URLSearchParams(window.location.search);
    return params.get("rentalId");
}

const rentalId = getRentalId();
if (!rentalId) {
    alert("Không tìm thấy mã thuê xe! Vui lòng đặt xe lại.");
    window.location.href = "/datxe";
}



// ================================
// LOAD THÔNG TIN
// ================================
let totalAmount = 0;
let rentalData = null;
let vehicleData = null;
let stationData = null;
let rentalDays = 1;

function formatDate(dateStr) {
    if (!dateStr) return "-";
    const d = new Date(dateStr);
    return d.toLocaleDateString("vi-VN");
}

async function loadRentalInfo() {
    try {
        const res = await fetch(`/api/rental/${rentalId}`);
        if (!res.ok) {
            const msg = await res.text();
            console.error("Lỗi khi gọi API rental", msg);
            if (res.status === 410) {
                alert(msg || "Đơn đặt đã hết hạn, vui lòng đặt lại.");
                window.location.href = "/datxe";
                return;
            }
            alert(msg || "Không tải được thông tin thanh toán, vui lòng thử lại.");
            return;
        }

        rentalData = await res.json();

        vehicleData = rentalData.vehicle || null;
        stationData = rentalData.station || null;

        if (!vehicleData) {
            const vehicleRes = await fetch(`/api/vehicles/${rentalData.vehicleId}`);
            vehicleData = vehicleRes.ok ? await vehicleRes.json() : null;
        }

        if (!stationData) {
            const stationRes = await fetch(`/api/stations/${rentalData.stationId}`);
            stationData = stationRes.ok ? await stationRes.json() : null;
        }

        document.querySelector(".username-label").innerText = rentalData.username || "";

        const brandPlate = vehicleData ? `${vehicleData.brand ?? vehicleData.type} (${vehicleData.plate})` : rentalData.vehicleId;
        const stationName = stationData ? `${stationData.name} - ${stationData.address ?? ""}` : rentalData.stationId || "";

        document.querySelector(".summary-value.rental-code").innerText = rentalData.id;
        const customerEl = document.querySelector(".summary-value.customer-name");
        if (customerEl) customerEl.innerText = rentalData.username || "-";
        document.querySelector(".summary-value.vehicle-type").innerText = brandPlate;
        document.querySelector(".summary-value.station-name").innerText = stationName;

        rentalDays = rentalData.rentalDays && rentalData.rentalDays > 0
            ? rentalData.rentalDays
            : Math.max(1, Math.ceil((new Date(rentalData.endTime) - new Date(rentalData.startTime)) / (1000 * 60 * 60 * 24)));
        const startLabel = formatDate(rentalData.startDate || rentalData.startTime);
        const endLabel = formatDate(rentalData.endDate || rentalData.endTime);

        document.querySelector(".summary-value.time-range").innerText = `${startLabel} - ${endLabel} (${rentalDays} ngày)`;
        document.querySelector(".summary-value.distance").innerText =
            rentalData.distanceKm ? `${Number(rentalData.distanceKm).toFixed(1)} km` : "-";

        const unitPrice =
            vehicleData?.price ||
            rentalData.vehiclePrice ||
            (rentalData.total && rentalData.rentalDays ? rentalData.total / rentalData.rentalDays : 0);

        const basePrice = unitPrice * rentalDays;
        totalAmount = rentalData.total && rentalData.total > 0
            ? rentalData.total
            : basePrice + (rentalData.damageFee ?? 0);

        document.querySelector(".detail-value.rental-days").innerText = `${rentalDays} ngày`;
        document.querySelector(".detail-value.basic-fee").innerText = basePrice.toLocaleString("vi-VN") + " VNĐ";
        document.querySelector(".detail-value.total-fee").innerText = totalAmount.toLocaleString("vi-VN") + " VNĐ";

        const methodSelect = document.getElementById("payment-method");
        if (rentalData.paymentMethod) {
            methodSelect.value = rentalData.paymentMethod;
        }
        document.getElementById("payment-method-text").innerText =
            (methodSelect.value || rentalData.paymentMethod) === "bank_transfer" ? "Chuyển khoản" : "Tiền mặt";

        refreshUploadStatus();
    } catch (err) {
        console.error("Lỗi loadRentalInfo:", err);
    }
}

function applyUploadState(data) {
    const statusText = document.getElementById("upload-status-text");
    const btnId = document.getElementById("btn-upload-id");
    const btnLicense = document.getElementById("btn-upload-license");
    if (!statusText || !btnId || !btnLicense || !data) return;

    if (data.licenseUploaded && data.idCardUploaded) {
        statusText.innerText = "Đã tải đủ CCCD & GPLX";
        statusText.style.color = "#2e7d32";
        btnId.style.display = "none";
        btnLicense.style.display = "none";
    } else {
        const missing = [!data.idCardUploaded ? "CCCD" : null, !data.licenseUploaded ? "GPLX" : null]
            .filter(Boolean)
            .join(", ");
        statusText.innerText = missing ? `Thiếu: ${missing}` : "Thiếu giấy tờ";
        statusText.style.color = "#c0392b";
        btnId.style.display = data.idCardUploaded ? "none" : "inline-flex";
        btnLicense.style.display = data.licenseUploaded ? "none" : "inline-flex";
    }
}

async function refreshUploadStatus(prefetched) {
    try {
        let data = prefetched;
        if (!data) {
            const res = await fetch("/api/renter/verification-status");
            if (!res.ok) return;
            data = await res.json();
        }
        applyUploadState(data);
    } catch (e) {
        console.error(e);
    }
}

function createUploader(type) {
    const input = document.createElement("input");
    input.type = "file";
    input.accept = "image/*";
    input.onchange = async () => {
        const form = new FormData();
        form.append("file", input.files[0]);
        const res = await fetch(`/api/renter/upload-${type}`, { method: "POST", body: form });
        if (res.ok) {
            const state = await res.json().catch(() => null);
            alert("Tải lên thành công");
            refreshUploadStatus(state);
        } else {
            const msg = await res.text();
            alert(msg || "Tải lên thất bại");
        }
    };
    input.click();
}

async function confirmPayment() {
    const method = document.getElementById("payment-method").value;
    if (!totalAmount || totalAmount <= 0) {
        alert("Không thể thanh toán số tiền bằng 0!");
        return;
    }

    if (method === "bank_transfer") {
        const statusRes = await fetch("/api/renter/verification-status");
        if (statusRes.ok) {
            const info = await statusRes.json();
            if (!info.licenseUploaded || !info.idCardUploaded) {
                alert("Vui lòng tải lên CCCD và GPLX trước khi chuyển khoản.");
                return;
            }
        }
    }

    const res = await fetch(`/api/rental/${rentalId}/payment`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ method })
    });

    if (!res.ok) {
        const message = await res.text();
        if (res.status === 410) {
            alert(message || "Đơn đặt đã hết hạn, vui lòng đặt lại.");
            window.location.href = "/datxe";
            return;
        }
        alert(message || "Thanh toán thất bại");
        return;
    }

    const data = await res.json();
    document.getElementById("payment-method-text").innerText =
        method === "bank_transfer" ? "Chuyển khoản" : "Tiền mặt";
    const latestTotal = data.total || totalAmount;
    document.querySelector(".detail-value.total-fee").innerText =
        latestTotal.toLocaleString("vi-VN") + " VNĐ";


    // ==============================
    // SEPAY
    // ==============================
    if (method === "bank_transfer" || data?.depositPending === true) {
        const qrRes = await fetch(`/payment/create-order?rentalId=${encodeURIComponent(rentalId)}`, {
            method: "POST"
        });

        if (!qrRes.ok) {
            const msg = await qrRes.text();
            console.error(msg);
            alert("Không tạo được QR thanh toán SePay!");
            return;
        }

        const qr = await qrRes.json();

        document.getElementById("qrImage").src = qr.qrBase64 || qr.qrUrl;
        document.getElementById("qrAmount").innerText = qr.amount.toLocaleString("vi-VN");
        document.getElementById("qrAccountName").innerText = qr.accountName;
        document.getElementById("qrAccountNumber").innerText = qr.accountNumber;
        document.getElementById("qrOrder").innerText = rentalId;

        document.getElementById("qrModal").style.display = "flex";
        if (method === "cash") {
            alert("Vui lòng chuyển khoản 30% đặt cọc để giữ xe. Phần còn lại sẽ thanh toán tại trạm.");
        }
        return;
    }

    alert("Đã lưu phương thức thanh toán. Vui lòng tới trạm để hoàn tất thanh toán tiền mặt!");
}

function cancelPayment() {
    fetch(`/api/rental/${rentalId}/cancel`, { method: "POST" }).finally(() => {
        window.location.href = "/datxe";
    });
}

function closeQR() {
    document.getElementById("qrModal").style.display = "none";
}

async function checkPaymentStatus() {
    const res = await fetch(`/api/payment/check?rentalId=${encodeURIComponent(rentalId)}`);
    if (!res.ok) {
        alert("Không kiểm tra được trạng thái thanh toán!");
        return;
    }

    const data = await res.json();

    if (data?.paid === true) {
        alert("Thanh toán thành công!");
        window.location.href = "/lichsuthue";
    } else {
        alert("Chưa nhận được thanh toán! Vui lòng đợi ngân hàng xử lý (1–3s).");
    }
}

document.addEventListener("DOMContentLoaded", () => {
    loadRentalInfo();

    document.querySelector(".btn-confirm-payment").onclick = confirmPayment;
    document.querySelector(".btn-cancel-payment").onclick = cancelPayment;
    document.getElementById("btn-upload-id").onclick = () => createUploader("idcard");
    document.getElementById("btn-upload-license").onclick = () => createUploader("license");
});
