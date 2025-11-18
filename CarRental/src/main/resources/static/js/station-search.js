let cachedStations = null;

function toRad(value) {
    return (value * Math.PI) / 180;
}

function calcDistanceKm(lat1, lon1, lat2, lon2) {
    const R = 6371;
    const dLat = toRad(lat2 - lat1);
    const dLon = toRad(lon2 - lon1);
    const a = Math.sin(dLat / 2) ** 2 + Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLon / 2) ** 2;
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
}

async function fetchStations() {
    if (cachedStations) return cachedStations;
    const res = await fetch("/api/stations");
    if (!res.ok) throw new Error("Không tải được danh sách trạm.");
    cachedStations = await res.json();
    return cachedStations;
}

function findStationByQuery(query, stations) {
    const normalized = query.trim().toLowerCase();
    if (!normalized) return null;

    const exact = stations.find(st => (st.name || "").toLowerCase() === normalized);
    if (exact) return exact;

    return stations.find(st => (st.name || "").toLowerCase().includes(normalized) || (st.address || "").toLowerCase().includes(normalized)) || null;
}

async function selectStationFromSearch({ input, hintEl }) {
    if (!input) return;

    try {
        if (hintEl) hintEl.textContent = "Đang tìm trạm...";
        const stations = await fetchStations();
        const match = findStationByQuery(input.value, stations);

        if (!match) {
            if (hintEl) hintEl.textContent = "Không tìm thấy trạm phù hợp.";
            return;
        }

        input.value = match.name;
        if (hintEl) hintEl.textContent = match.address || "Đã chọn trạm.";

        const currentUrl = new URL(window.location.href);
        if (currentUrl.pathname.startsWith("/datxe")) {
            const currentId = currentUrl.searchParams.get("stationId");
            if (currentId === match.id) return;
            currentUrl.searchParams.set("stationId", match.id);
            window.location.href = currentUrl.toString();
            return;
        }

        window.location.href = `/datxe?stationId=${match.id}`;
    } catch (err) {
        console.error(err);
        if (hintEl) hintEl.textContent = "Lỗi khi tìm kiếm trạm.";
    }
}

async function locateNearestStation({ inputId = "station-search", buttonId = "btn-find-station", hintId = "station-hint" } = {}) {
    const input = document.getElementById(inputId);
    const hintEl = document.getElementById(hintId);
    if (!input || !hintEl) return;

    hintEl.textContent = "Đang tìm trạm gần nhất...";

    if (!navigator.geolocation) {
        hintEl.textContent = "Trình duyệt không hỗ trợ định vị.";
        return;
    }

    navigator.geolocation.getCurrentPosition(async (pos) => {
        try {
            const stations = await fetchStations();
            if (!stations.length) {
                hintEl.textContent = "Chưa có trạm nào.";
                return;
            }

            const { latitude, longitude } = pos.coords;
            let best = null;
            let bestDistance = Infinity;

            stations.forEach(st => {
                if (st.latitude == null || st.longitude == null) return;
                const distance = calcDistanceKm(latitude, longitude, st.latitude, st.longitude);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = st;
                }
            });

            if (best) {
                input.value = best.name;
                const distanceText = Number.isFinite(bestDistance) ? ` (~${bestDistance.toFixed(1)} km)` : "";
                hintEl.textContent = `Gợi ý: ${best.address || ""}${distanceText}`;
            } else {
                hintEl.textContent = "Không tìm được trạm phù hợp.";
            }
        } catch (err) {
            console.error(err);
            hintEl.textContent = "Lỗi khi tìm trạm gần bạn.";
        }
    }, () => {
        hintEl.textContent = "Không thể truy cập vị trí của bạn.";
    }, { enableHighAccuracy: true, timeout: 8000 });
}

function setupStationSearch({ inputId = "station-search", buttonId = "btn-find-station", hintId = "station-hint" } = {}) {
    const button = document.getElementById(buttonId);
    const input = document.getElementById(inputId);
    const hintEl = document.getElementById(hintId);

    if (button) {
        button.addEventListener("click", () => locateNearestStation({ inputId, buttonId, hintId }));
    }

    if (input) {
        input.addEventListener("keydown", (evt) => {
            if (evt.key !== "Enter") return;
            evt.preventDefault();
            selectStationFromSearch({ input, hintEl });
        });
    }
}

document.addEventListener("DOMContentLoaded", () => {
    setupStationSearch();
});
