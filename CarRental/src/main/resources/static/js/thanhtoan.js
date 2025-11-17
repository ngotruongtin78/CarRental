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

async function loadRentalInfo() {
    try {
        const res = await fetch(`/api/rental/${rentalId}`);
        if (!res.ok) {
            console.error("Lỗi khi gọi API rental");
            return;
        }

        const rental = await res.json();

        const vehicleRes = await fetch(`/api/vehicles/admin/${rental.vehicleId}`);
        const vehicle = await vehicleRes.json();

        const stationRes = await fetch(`/api/stations/admin/${rental.stationId}`);
        const station = await stationRes.json();
        document.querySelector(".summary-value.rental-code").innerText = rental.id;
        document.querySelector(".summary-value.vehicle-type").innerText =
            `${vehicle.type} (${vehicle.plate})`;
        document.querySelector(".summary-value.station-name").innerText = station.name;


        // ================================
        // TÍNH SỐ NGÀY THUÊ
        // ================================
        const start = new Date(rental.startTime);
        const end = rental.endTime ? new Date(rental.endTime) : new Date();

        const diffMs = end - start;
        const days = Math.ceil(diffMs / (1000 * 60 * 60 * 24));

        document.querySelector(".summary-value.time-range").innerText =
            `${start.toLocaleDateString()} - ${end.toLocaleDateString()} (${days} ngày)`;


        // ================================
        // TÍNH TIỀN
        // ================================
        const dailyPrice = vehicle.price;
        const basePrice = days * dailyPrice;
        const damageFee = rental.damageFee ?? 0;

        document.querySelector(".detail-value.basic-fee").innerText =
            basePrice.toLocaleString("vi-VN") + " VNĐ";

        document.querySelector(".detail-value.damage-fee").innerText =
            damageFee.toLocaleString("vi-VN") + " VNĐ";

        totalAmount = basePrice + damageFee;

        document.querySelector(".detail-value.total-fee").innerText =
            totalAmount.toLocaleString("vi-VN") + " VNĐ";

    } catch (err) {
        console.error("Lỗi loadRentalInfo:", err);
    }
}


async function createPayOSPayment() {
    try {
        const payload = {
            orderCode: Number(Date.now().toString().slice(-8)), // 8 số cuối timestamp
            amount: totalAmount,
            description: `Thanh toán thuê xe mã ${rentalId}`,
            returnUrl: `http://localhost:8080/thanhtoan/success?rentalId=${rentalId}`,
            cancelUrl: `http://localhost:8080/thanhtoan/cancel?rentalId=${rentalId}`
        };

        const res = await fetch("/payment/create-order", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });

        const data = await res.json();

        if (!data || !data.checkoutUrl) {
            alert("Không tạo được link thanh toán!");
            return;
        }
        window.location.href = data.checkoutUrl;

    } catch (err) {
        console.error("PAYOS ERROR:", err);
        alert("Có lỗi khi tạo thanh toán!");
    }
}

async function confirmPayment() {
    if (!totalAmount || totalAmount <= 0) {
        alert("Không thể thanh toán số tiền bằng 0!");
        return;
    }
    await createPayOSPayment();
}

function cancelPayment() {
    window.location.href = "/datxe";
}

document.addEventListener("DOMContentLoaded", () => {
    convertHTMLPlaceholders();
    loadRentalInfo();

    document.querySelector(".btn-confirm-payment").onclick = confirmPayment;
    document.querySelector(".btn-cancel-payment").onclick = cancelPayment;
});

function convertHTMLPlaceholders() {
    document.querySelector(".summary-item:nth-child(1) .summary-value").classList.add("rental-code");
    document.querySelector(".summary-item:nth-child(3) .summary-value").classList.add("vehicle-type");
    document.querySelector(".summary-item:nth-child(4) .summary-value").classList.add("station-name");
    document.querySelector(".summary-item:nth-child(5) .summary-value").classList.add("time-range");

    document.querySelector(".payment-detail-row:nth-child(1) .detail-value").classList.add("basic-fee");
    document.querySelector(".payment-detail-row:nth-child(2) .detail-value").classList.add("damage-fee");
    document.querySelector(".payment-detail-row.total-amount .detail-value").classList.add("total-fee");
}
