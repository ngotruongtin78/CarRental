// ===============================
// LẤY stationId
// ===============================
function getStationId() {
    const params = new URLSearchParams(window.location.search);
    return params.get("stationId");
}

let selectedStation = getStationId();
let selectedVehicle = null;
let selectedVehicleData = null;
let stationCache = [];
let userCoordinates = null;
let documentInfo = null;

function getTodayLocalDate() {
    const now = new Date();
    const tzOffset = now.getTimezoneOffset() * 60000;
    const local = new Date(now.getTime() - tzOffset);
    return local.toISOString().split("T")[0];
}

// Helper function to format a date to YYYY-MM-DD without timezone issues
function formatLocalDate(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

// Helper function to get next day from a date string (YYYY-MM-DD)
function getNextDayStr(dateStr) {
    const date = new Date(dateStr + 'T00:00:00');
    date.setDate(date.getDate() + 1);
    return formatLocalDate(date);
}

function initDates() {
    const today = getTodayLocalDate();
    const startInput = document.getElementById("start-date");
    const endInput = document.getElementById("end-date");
    if (startInput) {
        startInput.value = today;
        startInput.min = today;
    }
    if (endInput) {
        // Ngày kết thúc mặc định là ngày mai (để đảm bảo ít nhất 1 ngày thuê)
        const tomorrowStr = getNextDayStr(today);
        endInput.value = tomorrowStr;
        endInput.min = tomorrowStr;
    }
}

function requestUserLocation() {
    if (!navigator.geolocation) return;
    navigator.geolocation.getCurrentPosition(pos => {
        userCoordinates = {
            lat: pos.coords.latitude,
            lon: pos.coords.longitude
        };
        updateDistanceDisplay();
    });
}

function getRentalDays() {
    const startInput = document.getElementById("start-date");
    const endInput = document.getElementById("end-date");
    if (!startInput || !endInput) return 1;
    const start = new Date(startInput.value);
    const end = new Date(endInput.value);
    // Tính số ngày thuê: end - start (không cộng 1)
    const diff = Math.ceil((end - start) / (1000 * 60 * 60 * 24));
    return diff > 0 ? diff : 1;
}

function renderSelectionDetails() {
    const nameEl = document.getElementById("sel-name");
    const priceEl = document.getElementById("sel-price");
    const statusEl = document.getElementById("sel-status");
    if (!nameEl || !priceEl || !statusEl) return;

    if (!selectedVehicleData) {
        nameEl.innerText = "Chưa chọn";
        priceEl.innerText = "-";
        statusEl.innerText = "-";
        statusEl.className = "status-badge";
        return;
    }

    nameEl.innerText = `${selectedVehicleData.brand ?? selectedVehicleData.type} (${selectedVehicleData.plate})`;
    const days = getRentalDays();
    const price = (selectedVehicleData.price ?? 0) * days;
    priceEl.innerText = `${(selectedVehicleData.price ?? 0).toLocaleString('vi-VN')} đ/ngày • ${price.toLocaleString('vi-VN')} đ/${days} ngày`;
    const bookingStatus = selectedVehicleData.bookingStatus || "AVAILABLE";
    statusEl.innerText = bookingStatus === "PENDING_PAYMENT" ? "Đang chờ thanh toán" : "Sẵn sàng";
    statusEl.className = "status-badge " + (bookingStatus === "PENDING_PAYMENT" ? "pending" : "available");
}

function calculateDistanceKm(lat1, lon1, lat2, lon2) {
    const R = 6371;
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLon = (lon2 - lon1) * Math.PI / 180;
    const a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
        Math.sin(dLon / 2) * Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return Math.round(R * c * 10) / 10; // 1 decimal
}

function updateDistanceDisplay() {
    const distanceEl = document.getElementById("distance-display");
    if (!distanceEl) return;

    if (!selectedStation || !userCoordinates) {
        distanceEl.innerText = "Chưa xác định";
        return;
    }

    const station = stationCache.find(s => s.id === selectedStation);
    if (!station) {
        distanceEl.innerText = "Chưa xác định";
        return;
    }

    const km = calculateDistanceKm(userCoordinates.lat, userCoordinates.lon, station.latitude, station.longitude);
    distanceEl.innerText = `${km} km`;
    distanceEl.dataset.value = km;
}

async function checkLogin() {
    try {
        const res = await fetch("/api/renter/profile");
        return res.ok;
    } catch {
        return false;
    }
}

function applyDocumentStatus(data) {
    const card = document.getElementById("document-upload-card");
    const statusEl = document.getElementById("document-status");
    const btnId = document.getElementById("btn-upload-idcard");
    const btnLicense = document.getElementById("btn-upload-license");
    const verifyText = document.getElementById("verify-status");
    const verifyBtn = document.getElementById("btn-request-verify");
    if (!card || !statusEl || !btnId || !btnLicense) return;

    const licenseUploaded = !!data?.licenseUploaded;
    const idCardUploaded = !!data?.idCardUploaded;

    card.style.display = "block";
    const missing = [];
    if (!idCardUploaded) missing.push("CCCD");
    if (!licenseUploaded) missing.push("GPLX");

    statusEl.innerText = missing.length ? `Vui lòng tải: ${missing.join(", ")}` : "Đã tải đủ giấy tờ.";
    statusEl.style.color = missing.length ? "#c0392b" : "#1f6d1f";
    btnId.style.display = idCardUploaded ? "none" : "inline-flex";
    btnLicense.style.display = licenseUploaded ? "none" : "inline-flex";

    if (verifyText && verifyBtn) {
        if (data?.verified) {
            verifyText.innerText = "Đã xác thực giấy tờ. Bạn có thể nhận xe nhanh.";
            verifyText.style.color = "#1f6d1f";
            verifyBtn.style.display = "none";
        } else if (data?.verificationRequested) {
            verifyText.innerText = "Đã gửi yêu cầu xác thực, vui lòng gặp nhân viên tại quầy.";
            verifyText.style.color = "#c47a0b";
            verifyBtn.disabled = true;
            verifyBtn.innerText = "Đã yêu cầu xác thực";
            verifyBtn.style.display = "inline-flex";
        } else {
            verifyText.innerText = "Chưa xác thực. Nhấn để nhờ nhân viên xác nhận nhanh tại điểm thuê.";
            verifyText.style.color = "#c0392b";
            verifyBtn.disabled = false;
            verifyBtn.innerText = "Yêu cầu xác thực tại quầy";
            verifyBtn.style.display = "inline-flex";
        }
    }
}

async function loadDocumentStatus() {
    try {
        const res = await fetch("/api/renter/verification-status");
        if (!res.ok) {
            applyDocumentStatus({});
            return;
        }
        const data = await res.json();
        documentInfo = data;
        applyDocumentStatus(data);
    } catch (e) {
        console.error("Lỗi kiểm tra giấy tờ:", e);
        applyDocumentStatus({});
    }
}

async function requestVerification() {
    try {
        const res = await fetch("/api/renter/request-verification", { method: "POST" });
        if (res.ok) {
            alert("Đã gửi yêu cầu xác thực. Vui lòng gặp nhân viên tại trạm để kiểm tra nhanh.");
            loadDocumentStatus();
        } else {
            const text = await res.text();
            alert(text || "Không gửi được yêu cầu xác thực.");
        }
    } catch (e) {
        alert("Không gửi được yêu cầu xác thực.");
    }
}

function triggerDocumentUpload(type) {
    const input = document.createElement("input");
    input.type = "file";
    input.accept = "image/*";
    input.onchange = async () => {
        if (!input.files.length) return;
        const form = new FormData();
        form.append("file", input.files[0]);
        const res = await fetch(`/api/renter/upload-${type}`, { method: "POST", body: form });
        if (res.ok) {
            const data = await res.json().catch(() => null);
            alert("Tải lên thành công");
            documentInfo = data || documentInfo;
            applyDocumentStatus(documentInfo || {});
        } else {
            const msg = await res.text();
            alert(msg || "Tải lên thất bại");
        }
    };
    input.click();
}

// ===============================
// LOAD DANH SÁCH TRẠM
// ===============================
async function loadStations() {
    try {
        const res = await fetch("/api/stations");
        if (!res.ok) return console.error("Không gọi được /api/stations");

        const stations = await res.json();
        stationCache = stations;
        const container = document.getElementById("station-list");
        container.innerHTML = "";

        stations.forEach(st => {
            const div = document.createElement("div");
            div.classList.add("station-item");

            div.innerHTML = `
                <i class="fa-solid fa-location-dot"></i>
                <b>${st.name}</b>
                <small style="display:block;color:#555;">${st.address ?? ""}</small>
            `;

            div.onclick = () => {
                selectedStation = st.id;
                selectedVehicle = null;
                selectedVehicleData = null;

                document.getElementById("selected-station").innerText = `${st.name} - ${st.address ?? ""}`;

                document.querySelectorAll(".station-item")
                    .forEach(el => el.classList.remove("active-station"));

                div.classList.add("active-station");

                loadVehicles();
                updateDistanceDisplay();
                renderSelectionDetails();
            };

            container.appendChild(div);
        });

        if (selectedStation) {
            const stObj = stations.find(s => s.id === selectedStation);

            if (stObj) {
                document.getElementById("selected-station").innerText = `${stObj.name} - ${stObj.address ?? ""}`;

                document.querySelectorAll(".station-item")
                    .forEach(el => {
                        if (el.querySelector("b").innerText === stObj.name) {
                            el.classList.add("active-station");
                            el.scrollIntoView({ behavior: "smooth", block: "center" });
                        }
                    });

                loadVehicles();
                updateDistanceDisplay();
            }
        }

    } catch (e) {
        console.error("Lỗi loadStations:", e);
    }
}

// ===============================
// LOAD XE THEO TRẠM
// ===============================
async function loadVehicles() {
    const list = document.getElementById("vehicle-list");

    if (!selectedStation) {
        list.innerHTML = "<p>Chưa chọn điểm thuê.</p>";
        return;
    }

    try {
        const res = await fetch(`/api/vehicles/station/${selectedStation}`);
        if (!res.ok) {
            list.innerHTML = "<p>Không tải được danh sách xe.</p>";
            return;
        }

        const vehicles = await res.json();
        list.innerHTML = "";

        if (!vehicles.length) {
            list.innerHTML = "<p>Không có xe nào tại trạm này.</p>";
            return;
        }

        vehicles.forEach(v => {
            const bookingStatus = v.bookingStatus || "AVAILABLE";
            if (bookingStatus === "RENTED") return; // ẩn xe đã thuê

            const div = document.createElement("div");
            div.classList.add("vehicle-card");

            const priceValue = v.price ?? 0;
            const isPending = bookingStatus === "PENDING_PAYMENT" && v.pendingRentalId;

            div.innerHTML = `
                <div class="vehicle-icon"><i class="fas fa-car"></i></div>
                <div>
                    <h3>${v.brand ? v.brand : ""} ${v.type} (${v.plate})</h3>
                    <p>Biển số: <b>${v.plate}</b></p>
                    <p>Pin: ${v.battery}%</p>
                    <p class="price-text">${priceValue.toLocaleString('vi-VN')}đ / ngày</p>
                    <p class="status-text ${isPending ? 'pending' : 'available'}">${isPending ? 'Đang chờ thanh toán' : 'Sẵn sàng'}</p>
                </div>
            `;

            if (isPending) {
                div.classList.add("vehicle-pending");
                div.onclick = () => alert("Xe đang chờ thanh toán. Vui lòng chọn xe khác!");
            } else {
                div.onclick = () => {
                    selectedVehicle = v.id;
                    selectedVehicleData = v;

                    document.querySelectorAll(".vehicle-card")
                        .forEach(el => el.classList.remove("selected"));

                    div.classList.add("selected");
                    renderSelectionDetails();
                };
            }

            list.appendChild(div);
        });

    } catch (e) {
        console.error("Lỗi loadVehicles:", e);
        list.innerHTML = "<p>Có lỗi khi tải xe.</p>";
    }
}

// ===============================
// ĐẶT XE
// ===============================
async function bookNow() {
    const loggedIn = await checkLogin();
    if (!loggedIn) {
        alert("Bạn cần đăng nhập để đặt xe!");
        window.location.href = "/login";
        return;
    }

    if (!selectedVehicle) {
        alert("Bạn phải chọn 1 xe trước khi đặt!");
        return;
    }

    if (!selectedStation) {
        alert("Vui lòng chọn điểm thuê trước khi đặt!");
        return;
    }

    try {
        const form = new FormData();
        form.append("vehicleId", selectedVehicle);
        form.append("stationId", selectedStation ?? "");
        form.append("startDate", document.getElementById("start-date").value);
        form.append("endDate", document.getElementById("end-date").value);

        const distanceValue = document.getElementById("distance-display")?.dataset?.value;
        if (distanceValue) {
            form.append("distanceKm", distanceValue);
        }

        const res = await fetch("/api/rental/book", {
            method: "POST",
            body: form
        });

        const text = await res.text();

        try {
            const rental = JSON.parse(text);

            if (!res.ok) {
                alert(rental);
                return;
            }

            if (!rental || !rental.id) {
                alert("Lỗi từ server!");
                return;
            }

            window.location.href = `/thanhtoan?rentalId=${rental.id}`;
        } catch (err) {
            alert(text);
        }

    } catch (e) {
        console.error("Lỗi khi đặt xe:", e);
        alert("Có lỗi khi đặt xe.");
    }
}

document.getElementById("btn-book").onclick = bookNow;
const btnIdCard = document.getElementById("btn-upload-idcard");
const btnLicense = document.getElementById("btn-upload-license");
const btnRequestVerify = document.getElementById("btn-request-verify");
if (btnIdCard) btnIdCard.onclick = () => triggerDocumentUpload("idcard");
if (btnLicense) btnLicense.onclick = () => triggerDocumentUpload("license");
if (btnRequestVerify) btnRequestVerify.onclick = requestVerification;

initDates();
requestUserLocation();
loadStations();
renderSelectionDetails();
loadDocumentStatus();

const startInput = document.getElementById("start-date");
const endInput = document.getElementById("end-date");
if (startInput) {
    startInput.addEventListener("change", () => {
        const today = getTodayLocalDate();
        if (startInput.value < today) {
            startInput.value = today;
        }
        if (endInput) {
            // endDate phải sau startDate ít nhất 1 ngày
            const nextDayStr = getNextDayStr(startInput.value);
            endInput.min = nextDayStr;
            
            // Auto set endDate = startDate + 1 nếu endDate <= startDate
            if (endInput.value <= startInput.value) {
                endInput.value = nextDayStr;
            }
        }
        renderSelectionDetails();
    });
}
if (endInput) {
    endInput.addEventListener("change", () => {
        if (startInput) {
            // Validation: Ngày kết thúc phải sau ngày bắt đầu ít nhất 1 ngày
            if (endInput.value <= startInput.value) {
                alert("Ngày kết thúc phải sau ngày bắt đầu ít nhất 1 ngày.\n\nVí dụ: Thuê ngày 29/11 → Trả ngày 30/11 = 1 ngày");
                // Reset to next day
                endInput.value = getNextDayStr(startInput.value);
            }
        }
        renderSelectionDetails();
    });
}
