document.addEventListener("DOMContentLoaded", function() {

    const historyTableBody = document.getElementById('historyTableBody');

    loadHistory();

    async function loadHistory() {
        try {
            const response = await fetch('/api/rentals/admin/all-history');

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            const rentals = await response.json();

            historyTableBody.innerHTML = '';

            rentals.forEach(rental => {
                const tr = document.createElement('tr');

                let statusClass = '';
                let statusText = rental.status;

                if (rental.status === 'PENDING') {
                    statusClass = 'status-pending';
                } else if (rental.status === 'RETURNED') {
                    statusClass = 'status-available';
                } else if (rental.status === 'CHECKED_IN') {
                    statusClass = 'status-rented';
                }

                tr.innerHTML = `
                    <td>${rental.id}</td>
                    <td>${rental.userId}</td>
                    <td>${rental.vehicleId}</td>
                    <td>${rental.stationId}</td>
                    <td><span class="status ${statusClass}">${statusText}</span></td>
                    <td>${(rental.totalPrice || 0).toLocaleString('vi-VN')}</td>
                `;
                historyTableBody.appendChild(tr);
            });

        } catch (error) {
            console.error("Không thể tải lịch sử thuê xe:", error);
            historyTableBody.innerHTML = '<tr><td colspan="6">Lỗi khi tải dữ liệu.</td></tr>';
        }
    }
});