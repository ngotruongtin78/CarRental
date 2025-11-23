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

            const statusInfo = getStatusInfo(rental);
            const userDisplay = rental.username || rental.userId || "N/A";

            tr.innerHTML = `
                <td>${rental.id}</td>
                <td>${userDisplay}</td>
                <td>${rental.vehicleId}</td>
                <td>${rental.stationId}</td>
                <td><span class="status ${statusInfo.className}">${statusInfo.text}</span></td>
                <td>${(rental.total || 0).toLocaleString('vi-VN')}</td>
            `;
            historyTableBody.appendChild(tr);
        });
    }

    function getStatusInfo(rental) {
        const status = (rental.status || '').toUpperCase();
        const paymentStatus = (rental.paymentStatus || '').toUpperCase();
        const paymentMethod = (rental.paymentMethod || '').toLowerCase();

        const isCancelled = ['CANCELLED', 'EXPIRED'].includes(status) || ['CANCELLED', 'EXPIRED'].includes(paymentStatus);
        if (isCancelled) return { text: 'Đã hủy', className: 'status-disabled' };

        if (['RETURNED', 'COMPLETED'].includes(status)) {
            return { text: 'Đã trả xe', className: 'status-available' };
        }
        if (status === 'WAITING_INSPECTION') {
            return { text: 'Chờ xác nhận trả', className: 'status-pending' };
        }
        if (paymentStatus === 'PAID' || status === 'PAID' || ['IN_PROGRESS', 'CHECKED_IN', 'CONTRACT_SIGNED'].includes(status)) {
            return { text: 'Đang thuê', className: 'status-available' };
        }
        if (paymentMethod === 'cash' || paymentStatus === 'PAY_AT_STATION' || status === 'PENDING_PAYMENT') {
            return { text: 'Đang chờ thanh toán', className: 'status-pending' };
        }

        return { text: 'Chờ xử lý', className: 'status-pending' };
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