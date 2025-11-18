document.addEventListener("DOMContentLoaded", function() {

    const historyTableBody = document.getElementById('historyTableBody');

    loadHistory();

    async function loadHistory() {
        try {
            const response = await fetch('/api/rental/admin/all-history');

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            const rentals = await response.json();

            historyTableBody.innerHTML = '';

            rentals.forEach(rental => {
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
                    case 'PENDING':
                        statusText = 'Chờ xử lý'; break;
                    case 'PENDING_PAYMENT':
                        statusText = 'Chờ thanh toán'; break;
                    case 'AWAITING_CASH':
                        statusText = 'Chờ tiền mặt'; break;
                    case 'PAID':
                        statusText = 'Đã thanh toán'; break;
                    case 'IN_PROGRESS':
                    case 'CHECKED_IN':
                        statusText = 'Đang thuê'; break;
                    case 'RETURNED':
                        statusText = 'Đã trả xe'; break;
                    case 'COMPLETED':
                        statusText = 'Hoàn thành'; break;
                    case 'CANCELLED':
                        statusText = 'Đã hủy'; break;
                    case 'EXPIRED':
                        statusText = 'Hết hạn'; break;
                    case 'CONTRACT_SIGNED':
                        statusText = 'Đã ký HĐ'; break;
                    default:
                        statusText = rental.status;
                }

                tr.innerHTML = `
                    <td>${rental.id}</td>
                    <td>${rental.username || rental.userId}</td>
                    <td>${rental.vehicleId}</td>
                    <td>${rental.stationId}</td>
                    <td><span class="status ${statusClass}">${statusText}</span></td>
                    <td>${(rental.total || 0).toLocaleString('vi-VN')}</td>
                `;
                historyTableBody.appendChild(tr);
            });

        } catch (error) {
            console.error("Không thể tải lịch sử thuê xe:", error);
            historyTableBody.innerHTML = '<tr><td colspan="6">Lỗi khi tải dữ liệu.</td></tr>';
        }
    }
});