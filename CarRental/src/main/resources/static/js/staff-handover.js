let allHandovers = [];
let currentRentalId = null;
let currentContractData = null;

// Hàm lấy danh sách xe giao
function loadHandovers() {
    fetch('/api/staff/handover/list')
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP Error: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            allHandovers = data || [];
            displayHandovers(allHandovers);
        })
        .catch(error => {
            console.error('Lỗi khi tải dữ liệu:', error);
            showError('Lỗi: ' + error.message);
        });
}

// Hàm hiển thị danh sách
function displayHandovers(handovers) {
    const tableBody = document.getElementById('tableBody');

    if (!handovers || handovers.length === 0) {
        tableBody.innerHTML = `
            <tr>
                <td colspan="6" class="no-data">
                    <div>
                        <i class="bi bi-inbox"></i>
                        <p style="margin-top: 15px;">Không có xe sẵn sàng giao</p>
                    </div>
                </td>
            </tr>
        `;
        return;
    }

    tableBody.innerHTML = handovers.map(item => {
        const statusLabel = getStatusLabel(item.paymentStatus);
        const statusClass = getStatusClass(item.paymentStatus);
        const formattedDate = formatDate(item.startDate);

        return `
            <tr>
                <td data-label="Biển số xe">
                    <strong>${item.licensePlate || 'N/A'}</strong>
                </td>
                <td data-label="Loại xe">${item.vehicleType || 'N/A'}</td>
                <td data-label="Tên người thuê">${item.renterName || 'N/A'}</td>
                <td data-label="Ngày thuê">${formattedDate}</td>
                <td data-label="Trạng thái thanh toán">
                    <span class="status-badge ${statusClass}">${statusLabel}</span>
                </td>
                <td data-label="Hành động" style="text-align: center;">
                    <button class="btn-detail" onclick="viewContractDetail('${item.id}')" title="Xem chi tiết">
                        <i class="bi bi-arrow-right"></i>
                    </button>
                </td>
            </tr>
        `;
    }).join('');
}

// Hàm mở modal chi tiết hợp đồng
function viewContractDetail(rentalId) {
    currentRentalId = rentalId;

    // Hiển thị modal với loading state
    document.getElementById('contractModal').style.display = 'block';
    document.getElementById('contractModalOverlay').style.display = 'block';

    // Tải dữ liệu chi tiết
    const apiUrl = `/api/staff/handover/${rentalId}`;

    fetch(apiUrl)
        .then(response => {
            if (!response.ok) {
                return response.text().then(text => {
                    throw new Error(`HTTP Error: ${response.status}`);
                });
            }
            return response.json();
        })
        .then(data => {
            if (data.error) {
                alert('Lỗi: ' + data.error);
                closeContractModal();
                return;
            }

            currentContractData = data;
            displayContractDetail(data);
        })
        .catch(error => {
            console.error('Lỗi fetch:', error);
            alert('Lỗi: ' + error.message);
            closeContractModal();
        });
}

// Hàm hiển thị chi tiết hợp đồng trong modal
function displayContractDetail(data) {
    document.getElementById('modal-plate').textContent = data.licensePlate || 'N/A';
    document.getElementById('modal-type').textContent = data.vehicleType || 'N/A';
    document.getElementById('modal-renter').textContent = data.renterName || 'N/A';
    document.getElementById('modal-start-date').textContent = data.startDate || 'N/A';
    document.getElementById('modal-end-date').textContent = data.endDate || 'N/A';
    document.getElementById('modal-payment-method').textContent = data.paymentMethod || 'N/A';
    document.getElementById('modal-price').textContent = formatPrice(data.total);

    // Định dạng trạng thái thanh toán
    const paymentStatus = data.paymentStatus || 'N/A';
    const statusText = paymentStatus.toUpperCase() === 'PAID' ? 'Đã thanh toán' :
                      paymentStatus.toUpperCase() === 'DEPOSIT_PENDING' ? 'Thanh toán tại trạm' : paymentStatus;
    document.getElementById('modal-payment-status').textContent = statusText;

    document.getElementById('modal-notes').textContent = data.checkinNotes || 'Không có ghi chú';

    document.getElementById('modal-checkin-time').textContent = formatDateTime(data.checkinTime) || '-';
    document.getElementById('modal-return-time').textContent = formatDateTime(data.returnTime) || '-';
    document.getElementById('modal-checkin-loc').textContent = formatLocation(data.checkinLatitude, data.checkinLongitude);
    document.getElementById('modal-return-loc').textContent = formatLocation(data.returnLatitude, data.returnLongitude);
}

function formatDateTime(value) {
    if (!value) return '';
    const dt = new Date(value);
    if (isNaN(dt)) return '';
    return `${dt.toLocaleDateString()} ${dt.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`;
}

function formatLocation(lat, lng) {
    if (lat === undefined || lng === undefined || lat === null || lng === null) return '-';
    return `${lat.toFixed(5)}, ${lng.toFixed(5)}`;
}

// Hàm đóng modal
function closeContractModal() {
    document.getElementById('contractModal').style.display = 'none';
    document.getElementById('contractModalOverlay').style.display = 'none';
    currentRentalId = null;
    currentContractData = null;
}

// Hàm xác nhận cho thuê
function confirmHandover() {
    if (!currentRentalId) {
        alert('Không có hợp đồng nào được chọn');
        return;
    }

    if (confirm('Bạn có chắc chắn muốn xác nhận giao xe này không?')) {
        fetch(`/api/staff/handover/confirm/${currentRentalId}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                alert('✓ Xác nhận giao xe thành công!');
                closeContractModal();
                loadHandovers(); // Tải lại danh sách
            } else {
                alert('Lỗi: ' + (data.message || 'Xác nhận thất bại'));
            }
        })
        .catch(error => {
            console.error('Lỗi khi xác nhận:', error);
            alert('Lỗi: ' + error.message);
        });
    }
}

// Hàm chuyển đổi trạng thái thanh toán
function getStatusLabel(status) {
    if (!status) return 'Không rõ';
    const s = status.trim().toUpperCase();
    if (s === 'PAID') return 'Đã thanh toán';
    if (s === 'DEPOSIT_PENDING') return 'Thanh toán tại trạm';
    return status;
}

// Hàm lấy CSS class cho trạng thái
function getStatusClass(status) {
    if (!status) return '';
    const s = status.trim().toUpperCase();
    if (s === 'PAID') return 'status-paid';
    if (s === 'DEPOSIT_PENDING') return 'status-pay-at-station';
    return '';
}

// Hàm định dạng ngày tháng
function formatDate(date) {
    if (!date) return 'N/A';
    const d = new Date(date);
    return d.toLocaleDateString('vi-VN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit'
    });
}

// Hàm định dạng giá tiền
function formatPrice(price) {
    if (!price) return '0 VND';
    return new Intl.NumberFormat('vi-VN').format(price) + ' VND';
}

// Hàm hiển thị lỗi
function showError(message) {
    const tableBody = document.getElementById('tableBody');
    tableBody.innerHTML = `
        <tr>
            <td colspan="6" class="no-data" style="color: #e74c3c;">
                <div>
                    <i class="bi bi-exclamation-circle"></i>
                    <p style="margin-top: 15px;">${message}</p>
                </div>
            </td>
        </tr>
    `;
}

// Tải danh sách khi trang load
window.addEventListener('DOMContentLoaded', function() {
    loadHandovers();
});

// Đóng modal khi nhấn Escape
window.addEventListener('keydown', function(event) {
    if (event.key === 'Escape') {
        closeContractModal();
    }
});

