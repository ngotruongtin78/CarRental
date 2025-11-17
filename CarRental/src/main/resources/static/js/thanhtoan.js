// ================================
// LẤY rentalId
// ================================
function getRentalId() {
    const params = new URLSearchParams(window.location.search);
    return params.get("rentalId");
}

const rentalId = getRentalId();
if (!rentalId) {
    alert("Không tìm thấy mã thuê xe!");
}



// ================================
// LOAD THÔNG TIN
// ================================
let totalAmount = 0;
let rentalData = null;
let vehicleData = null;
let stationData = null;

function formatDate(dateStr) {
    if (!dateStr) return "-";
    const d = new Date(dateStr);
    return d.toLocaleDateString("vi-VN");
}

async function loadRentalInfo() {
    try {
        const res = await fetch(`/api/rental/${rentalId}`);
        if (!res.ok) {
            console.error("Lỗi khi gọi API rental");
            alert("Không tải được thông tin thanh toán, vui lòng thử lại.");
            return;
        }

        rentalData = await res.json();

        const vehicleRes = await fetch(`/api/vehicles/admin/${rentalData.vehicleId}`);
        vehicleData = vehicleRes.ok ? await vehicleRes.json() : null;

        const stationRes = await fetch(`/api/stations/admin/${rentalData.stationId}`);
        stationData = stationRes.ok ? await stationRes.json() : null;

        const brandPlate = vehicleData ? `${vehicleData.brand ?? vehicleData.type} (${vehicleData.plate})` : rentalData.vehicleId;
        const stationName = stationData ? `${stationData.name} - ${stationData.address ?? ""}` : rentalData.stationId || "";

        document.querySelector(".summary-value.rental-code").innerText = rentalData.id;
        const customerEl = document.querySelector(".summary-value.customer-name");
        if (customerEl) customerEl.innerText = rentalData.username || "-";
        document.querySelector(".summary-value.vehicle-type").innerText = brandPlate;
        document.querySelector(".summary-value.station-name").innerText = stationName;

        const days = rentalData.rentalDays && rentalData.rentalDays > 0
            ? rentalData.rentalDays
            : Math.max(1, Math.ceil((new Date(rentalData.endTime) - new Date(rentalData.startTime)) / (1000 * 60 * 60 * 24)));
        const startLabel = formatDate(rentalData.startDate || rentalData.startTime);
        const endLabel = formatDate(rentalData.endDate || rentalData.endTime);

        document.querySelector(".summary-value.time-range").innerText = `${startLabel} - ${endLabel} (${days} ngày)`;
        document.querySelector(".summary-value.distance").innerText =
            rentalData.distanceKm ? `${Number(rentalData.distanceKm).toFixed(1)} km` : "-";

        const basePrice = (vehicleData?.price || 0) * days;
        totalAmount = basePrice + (rentalData.damageFee ?? 0);

        document.querySelector(".detail-value.basic-fee").innerText = basePrice.toLocaleString("vi-VN") + " VNĐ";
        document.querySelector(".detail-value.total-fee").innerText = totalAmount.toLocaleString("vi-VN") + " VNĐ";
        document.getElementById("payment-method-text").innerText = rentalData.paymentMethod === "bank_transfer" ? "Chuyển khoản" : "Tiền mặt";

        refreshUploadStatus();
    } catch (err) {
        console.error("Lỗi loadRentalInfo:", err);
    }
}

async function refreshUploadStatus() {
    try {
        const res = await fetch("/api/renter/verification-status");
        if (!res.ok) return;
        const data = await res.json();
        const statusText = document.getElementById("upload-status-text");
        if (!statusText) return;
        if (data.licenseUploaded && data.idCardUploaded) {
            statusText.innerText = "Đã đủ CCCD & GPLX";
            statusText.style.color = "#2e7d32";
        } else {
            statusText.innerText = "Thiếu giấy tờ";
            statusText.style.color = "#c0392b";
        }
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
            alert("Tải lên thành công");
            refreshUploadStatus();
        } else {
            alert("Tải lên thất bại");
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
        alert(message || "Thanh toán thất bại");
        return;
    }

    const data = await res.json();
    document.getElementById("payment-method-text").innerText = method === "bank_transfer" ? "Chuyển khoản" : "Tiền mặt";
    document.querySelector(".detail-value.total-fee").innerText = (data.total || totalAmount).toLocaleString("vi-VN") + " VNĐ";
    alert("Đã lưu phương thức thanh toán. Vui lòng tới trạm (tiền mặt) hoặc chuyển khoản theo hướng dẫn!");
}

function cancelPayment() {
    window.location.href = "/datxe";
}

document.addEventListener("DOMContentLoaded", () => {
    convertHTMLPlaceholders();
    loadRentalInfo();

    document.querySelector(".btn-confirm-payment").onclick = confirmPayment;
    document.querySelector(".btn-cancel-payment").onclick = cancelPayment;
    document.getElementById("btn-upload-id").onclick = () => createUploader("idcard");
    document.getElementById("btn-upload-license").onclick = () => createUploader("license");
});

function convertHTMLPlaceholders() {
    document.querySelector(".summary-item:nth-child(1) .summary-value").classList.add("rental-code");
    document.querySelector(".summary-item:nth-child(2) .summary-value").classList.add("customer-name");
    document.querySelector(".summary-item:nth-child(3) .summary-value").classList.add("vehicle-type");
    document.querySelector(".summary-item:nth-child(4) .summary-value").classList.add("station-name");
    document.querySelector(".summary-item:nth-child(5) .summary-value").classList.add("time-range");
    document.querySelector(".summary-item:nth-child(6) .summary-value").classList.add("distance");

    document.querySelector(".payment-detail-row:nth-child(1) .detail-value").classList.add("basic-fee");
    document.querySelector(".payment-detail-row.total-amount .detail-value").classList.add("total-fee");
}
