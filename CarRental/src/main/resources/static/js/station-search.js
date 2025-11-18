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
            const res = await fetch("/api/stations");
            if (!res.ok) {
                hintEl.textContent = "Không tải được danh sách trạm.";
                return;
            }
            const stations = await res.json();
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
                input.value = `${best.name} (${bestDistance.toFixed(1)} km)`;
                hintEl.textContent = `Gợi ý: ${best.address || ""}`;
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
    if (!button) return;
    button.addEventListener("click", () => locateNearestStation({ inputId, buttonId, hintId }));
}

document.addEventListener("DOMContentLoaded", () => {
    setupStationSearch();
});
