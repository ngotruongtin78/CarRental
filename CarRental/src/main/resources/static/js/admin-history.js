document.addEventListener("DOMContentLoaded", function() {

    const historyTableBody = document.getElementById('historyTableBody');
    const searchInput = document.getElementById('searchInput');

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
    });

    loadHistory();

    async function loadHistory() {
        if (!historyTableBody) return;
        historyTableBody.innerHTML = '<tr><td colspan="6" style="text-align:center;">Đang tải dữ liệu...</td></tr>';

        try {
            const response = await fetch('/api/rental/admin/all-history');

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            allRentalsData = await response.json();

            renderTable(allRentalsData);

        } catch (error) {
            console.error("Không thể tải lịch sử thuê xe:", error);
            historyTableBody.innerHTML = '<tr><td colspan="6" style="text-align:center; color:red;">Lỗi khi tải dữ liệu.</td></tr>';
        }
    }

    function renderTable(data) {
        historyTableBody.innerHTML = '';

        if (!data || data.length === 0) {
            historyTableBody.innerHTML = '<tr><td colspan="6" style="text-align:center;">Không tìm thấy đơn thuê nào.</td></tr>';
            return;
        }

        data.forEach(rental => {
            const tr = document.createElement('tr');

            let statusClass = 'status-inactive';
            if (['PENDING', 'PENDING_PAYMENT', 'AWAITING_CASH'].includes(rental.status)) {
                statusClass = 'status-pending';
            } else if (['RETURNED', 'COMPLETED', 'PAID', 'IN_PROGRESS', 'CHECKED_IN', 'CONTRACT_SIGNED'].includes(rental.status)) {
                statusClass = 'status-available';
            } else if (['CANCELLED', 'EXPIRED'].includes(rental.status)) {
                statusClass = 'status-disabled';
            }

            let statusText = rental.status;
            switch (rental.status) {
                case 'PENDING': statusText = 'Chờ xử lý'; break;
                case 'PENDING_PAYMENT': statusText = 'Chờ thanh toán'; break;
                case 'AWAITING_CASH': statusText = 'Chờ tiền mặt'; break;
                case 'PAID': statusText = 'Đã thanh toán'; break;
                case 'IN_PROGRESS':
                case 'CHECKED_IN': statusText = 'Đang thuê'; break;
                case 'RETURNED': statusText = 'Đã trả xe'; break;
                case 'COMPLETED': statusText = 'Hoàn thành'; break;
                case 'CANCELLED': statusText = 'Đã hủy'; break;
                case 'EXPIRED': statusText = 'Hết hạn'; break;
                case 'CONTRACT_SIGNED': statusText = 'Đã ký HĐ'; break;
                default: statusText = rental.status;
            }

            const userDisplay = rental.username || rental.userId || "N/A";

            tr.innerHTML = `
                <td>${rental.id}</td>
                <td>${userDisplay}</td>
                <td>${rental.vehicleId}</td>
                <td>${rental.stationId}</td>
                <td><span class="status ${statusClass}">${statusText}</span></td>
                <td>${(rental.total || 0).toLocaleString('vi-VN')}</td>
            `;
            historyTableBody.appendChild(tr);
        });
    }

    function filterHistory() {
        if (!allRentalsData) return;

        const searchText = searchInput.value.toLowerCase().trim();
        const filtered = allRentalsData.filter(rental => {
            const rentalId = (rental.id || "").toLowerCase();
            const username = (rental.username || "").toLowerCase();
            const userId = (rental.userId || "").toLowerCase();
            const vehicleId = (rental.vehicleId || "").toLowerCase();

            return rentalId.includes(searchText) ||
                   username.includes(searchText) ||
                   userId.includes(searchText) ||
                   vehicleId.includes(searchText);
        });

        renderTable(filtered);
    }

    if (searchInput) {
        searchInput.addEventListener('input', filterHistory);
    }
});