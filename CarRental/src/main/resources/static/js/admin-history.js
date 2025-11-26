document.addEventListener("DOMContentLoaded", function() {

    const historyTableBody = document.getElementById('historyTableBody');
    const searchInput = document.getElementById('searchInput');
    const detailModal = document.getElementById('rentalDetailModal');
    const detailContent = document.getElementById('detailContent');

    let allRentalsData = [];

    window.toggleProfileMenu = function(event) {
        event.stopPropagation();
        const dropdown = document.getElementById('profileDropdown');
        if (dropdown) dropdown.classList.toggle('show');
    };

    window.addEventListener('click', function(event) {
        if (!event.target.closest('.admin-profile')) {
            const dropdown = document.getElementById('profileDropdown');
            if (dropdown && dropdown.classList.contains('show')) {
                dropdown.classList.remove('show');
            }
        }
        if (event.target == detailModal) {
            detailModal.style.display = "none";
        }
    });

    window.closeDetailModal = function() {
        if (detailModal) detailModal.style.display = "none";
    };

    loadHistory();

    async function loadHistory() {
        if (!historyTableBody) return;
        historyTableBody.innerHTML = '<tr><td colspan="7" style="text-align:center;">Đang tải dữ liệu...</td></tr>';

        try {
            const response = await fetch('/api/rental/admin/all-history');
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);

            allRentalsData = await response.json();

            renderTable(allRentalsData);

        } catch (error) {
            console.error("Lỗi:", error);
            historyTableBody.innerHTML = '<tr><td colspan="7" style="text-align:center; color:red;">Lỗi khi tải dữ liệu.</td></tr>';
        }
    }

    function renderTable(data) {
        historyTableBody.innerHTML = '';

        if (!data || data.length === 0) {
            historyTableBody.innerHTML = '<tr><td colspan="7" style="text-align:center;">Không có đơn thuê nào.</td></tr>';
            return;
        }

        data.forEach(item => {
            const rental = item.record;
            const vehicle = item.vehicle || {};
            const station = item.station || {};

            const tr = document.createElement('tr');

            const displayStatus = item.displayStatus || "N/A";
            let statusClass = "status-pending";
            if (displayStatus.includes("Đang thuê")) statusClass = "status-available";
            if (displayStatus.includes("trả xe")) statusClass = "status-available";
            if (displayStatus.includes("chờ thanh toán")) statusClass = "status-pending";

            const userDisplay = rental.username || rental.userId || "N/A";

            const vehicleDisplay = vehicle.plate
                ? `<strong>${vehicle.plate}</strong><br><small>${vehicle.brand || ''}</small>`
                : `<span style="color:#999">${rental.vehicleId}</span>`;

            const stationDisplay = station.name
                ? station.name
                : rental.stationId;

            tr.innerHTML = `
                <td>${rental.id}</td>
                <td>${userDisplay}</td>
                <td>${vehicleDisplay}</td>
                <td>${stationDisplay}</td>
                <td><span class="status ${statusClass}">${displayStatus}</span></td>
                <td>${(rental.total || 0).toLocaleString('vi-VN')}</td>
                <td>
                    <button class="btn-view" onclick="viewDetail('${rental.id}')">
                        <i class="fas fa-eye"></i> Chi tiết
                    </button>
                </td>
            `;
            historyTableBody.appendChild(tr);
        });
    }

    window.viewDetail = async function(rentalId) {
        if (!detailModal) return;

        detailContent.innerHTML = '<p style="text-align:center">Đang tải thông tin...</p>';
        detailModal.style.display = "block";

        try {
            const res = await fetch(`/api/rental/admin/detail/${rentalId}`);
            if (!res.ok) throw new Error("Không tải được chi tiết");

            const data = await res.json();
            const r = data.rental || {};
            const v = data.vehicle || {};
            const s = data.station || {};

            const formatDate = (d) => d ? new Date(d).toLocaleString('vi-VN') : '---';

            const html = `
                <div class="section-title">Thông tin chung</div>
                <div class="detail-row"><span class="detail-label">Mã đơn:</span> <span class="detail-value">${r.id}</span></div>
                <div class="detail-row"><span class="detail-label">Trạng thái:</span> <span class="detail-value">${r.status}</span></div>
                <div class="detail-row"><span class="detail-label">Thanh toán:</span> <span class="detail-value">${r.paymentStatus} (${r.paymentMethod || 'N/A'})</span></div>
                <div class="detail-row"><span class="detail-label">Tổng tiền:</span> <span class="detail-value" style="font-weight:bold; color:#e74c3c">${(r.total || 0).toLocaleString('vi-VN')} đ</span></div>

                <div class="section-title">Khách hàng</div>
                <div class="detail-row"><span class="detail-label">Tài khoản:</span> <span class="detail-value">${r.username || 'N/A'}</span></div>
                <div class="detail-row"><span class="detail-label">ID:</span> <span class="detail-value">${r.userId || 'N/A'}</span></div>

                <div class="section-title">Thông tin Xe & Trạm</div>
                <div class="detail-row"><span class="detail-label">Xe:</span> <span class="detail-value">${v.brand || ''} ${v.type || ''} - <b>${v.plate || r.vehicleId}</b></span></div>
                <div class="detail-row"><span class="detail-label">Trạm thuê:</span> <span class="detail-value">${s.name || r.stationId}</span></div>
                <div class="detail-row"><span class="detail-label">Địa chỉ trạm:</span> <span class="detail-value">${s.address || '---'}</span></div>

                <div class="section-title">Thời gian</div>
                <div class="detail-row"><span class="detail-label">Bắt đầu:</span> <span class="detail-value">${formatDate(r.startTime)}</span></div>
                <div class="detail-row"><span class="detail-label">Kết thúc:</span> <span class="detail-value">${formatDate(r.endTime)}</span></div>

                <div class="section-title">Ghi chú</div>
                <div class="detail-row"><span class="detail-label">Check-in:</span> <span class="detail-value">${r.checkinNotes || 'Không có'}</span></div>
                <div class="detail-row"><span class="detail-label">Trả xe:</span> <span class="detail-value">${r.returnNotes || 'Không có'}</span></div>
            `;

            detailContent.innerHTML = html;

        } catch (e) {
            console.error(e);
            detailContent.innerHTML = '<p style="color:red; text-align:center">Lỗi khi tải dữ liệu chi tiết.</p>';
        }
    };

    function filterHistory() {
        if (!allRentalsData) return;

        const searchText = searchInput.value.toLowerCase().trim();
        const filtered = allRentalsData.filter(item => {
            const r = item.record;
            const v = item.vehicle || {};

            const rentalId = (r.id || "").toLowerCase();
            const username = (r.username || "").toLowerCase();
            const plate = (v.plate || "").toLowerCase();

            return rentalId.includes(searchText) ||
                   username.includes(searchText) ||
                   plate.includes(searchText);
        });

        renderTable(filtered);
    }

    if (searchInput) {
        searchInput.addEventListener('input', filterHistory);
    }
});