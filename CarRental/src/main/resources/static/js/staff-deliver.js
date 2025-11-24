// ========== STAFF DELIVER PAGE SCRIPT ==========

// Lưu thông tin rental hiện tại
let currentRentalId = null;

// Tải danh sách xe sẵn sàng giao khi trang load
document.addEventListener('DOMContentLoaded', function() {
    loadDeliveryVehicles();
});

/**
 * Lấy danh sách các xe sẵn sàng giao từ API
 * Điều kiện: paymentStatus = "PAID" hoặc "PAY_AT_STATION"
 */
function loadDeliveryVehicles() {
    fetch('/api/staff/deliver/vehicles-ready')
        .then(response => response.json())
        .then(data => {
            populateDeliveryTable(data);
        })
        .catch(error => {
            console.error('Lỗi khi tải danh sách xe:', error);
            alert('Lỗi khi tải danh sách xe sẵn sàng giao');
        });
}

/**
 * Điền dữ liệu vào bảng danh sách giao xe
 * Hiển thị: Biển số xe, Loại xe, Tên người thuê, Ngày thuê, Trạng thái thanh toán
 */
function populateDeliveryTable(rentalRecords) {
    const tableBody = document.getElementById('deliveryTableBody');
    tableBody.innerHTML = '';

    if (!rentalRecords || rentalRecords.length === 0) {
        tableBody.innerHTML = '<tr><td colspan="6" style="text-align: center; padding: 20px; color: #999;">Không có đơn hàng sẵn sàng giao</td></tr>';
        return;
    }

    rentalRecords.forEach((rental, index) => {
        const row = document.createElement('tr');

        // Xác định trạng thái thanh toán và màu badge
        let paymentStatusBadge = '';
        const paymentStatus = rental.paymentStatus || 'UNKNOWN';

        if (paymentStatus === 'PAID') {
            paymentStatusBadge = '<span class="status-badge status-paid">ĐÃ THANH TOÁN</span>';
        } else if (paymentStatus === 'PAY_AT_STATION') {
            paymentStatusBadge = '<span class="status-badge status-pending">THANH TOÁN TẠI TRẠM</span>';
        } else {
            paymentStatusBadge = '<span class="status-badge status-pending">CHƯA THANH TOÁN</span>';
        }

        // Format ngày từ startDate (LocalDate từ RentalRecord)
        const rentalDate = rental.startDate ? formatDate(rental.startDate) : 'N/A';
        const vehiclePlate = rental.vehiclePlate || 'N/A';
        const vehicleType = rental.vehicleType || 'N/A';
        const customerName = rental.username || 'N/A';

        row.innerHTML = `
            <td class="plate-cell">${vehiclePlate}</td>
            <td>${vehicleType}</td>
            <td>${customerName}</td>
            <td>${rentalDate}</td>
            <td>${paymentStatusBadge}</td>
            <td class="action-cell">
                <button class="btn-action btn-deliver-now" onclick="handleDeliverVehicle('${rental.id}', '${vehiclePlate}', '${customerName}')">
                    <span class="icon">→</span>
                </button>
            </td>
        `;

        tableBody.appendChild(row);
    });
}

/**
 * Format ngày từ định dạng ISO hoặc đối tượng Date
 * Chuyển đổi "2025-11-22" thành "22/11/2025"
 */
function formatDate(dateStr) {
    if (!dateStr) return 'N/A';

    let date;
    if (typeof dateStr === 'string') {
        date = new Date(dateStr);
    } else {
        date = new Date(dateStr);
    }

    if (isNaN(date)) return 'N/A';

    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const year = date.getFullYear();

    return `${day}/${month}/${year}`;
}

/**
 * Format tiền tệ thành định dạng VND
 * Ví dụ: 450000 → "450,000 VND"
 */
function formatCurrency(value) {
    if (!value) return 'N/A';
    return new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND'
    }).format(value);
}

/**
 * Format trạng thái thanh toán
 * "PAID" → "Đã thanh toán"
 * "PAY_AT_STATION" → "Thanh toán tại trạm"
 */
function formatPaymentStatus(status) {
    if (!status) return 'N/A';
    const statusMap = {
        'PAID': 'Đã thanh toán',
        'PAY_AT_STATION': 'Thanh toán tại trạm',
        'PENDING': 'Chờ thanh toán',
        'UNPAID': 'Chưa thanh toán'
    };
    return statusMap[status] || status;
}

/**
 * Mở modal giao xe với toàn bộ thông tin chi tiết
 */
function handleDeliverVehicle(rentalId, plate, customerName) {
    currentRentalId = rentalId;

    // Gọi API để lấy chi tiết hợp đồng
    fetch(`/api/staff/deliver/${rentalId}`)
        .then(response => response.json())
        .then(data => {
            // Điền thông tin xe
            document.getElementById('deliverPlate').value = data.vehiclePlate || plate;
            document.getElementById('deliverVehicleType').value = data.vehicleType || 'N/A';

            // Điền thông tin khách hàng
            document.getElementById('deliverCustomer').value = data.username || customerName;

            // Điền thông tin thuê xe
            document.getElementById('deliverStartDate').value = formatDate(data.startDate) || 'N/A';
            document.getElementById('deliverEndDate').value = formatDate(data.endDate) || 'N/A';
            document.getElementById('deliverRentalDays').value = (data.rentalDays || 0) + ' ngày';

            // Điền thông tin thanh toán
            document.getElementById('deliverTotal').value = formatCurrency(data.total) || 'N/A';
            document.getElementById('deliverPaymentStatus').value = formatPaymentStatus(data.paymentStatus) || 'N/A';

            // Làm trống ghi chú
            document.getElementById('deliverNote').value = '';

            // Reset camera state
            closeDeliveryCamera();
            document.getElementById('deliveryVideoStream').style.display = 'none';
            document.getElementById('deliveryPhotoPreview').style.display = 'none';
            document.getElementById('deliveryCameraControls').style.display = 'block';
            document.getElementById('deliveryCaptureControls').style.display = 'none';
            document.getElementById('deliveryRetakeControls').style.display = 'none';
            window.currentDeliveryPhotoBase64 = null;
            window.currentDeliveryPhotoFileName = null;

            // Mở modal
            document.getElementById('deliverModal').style.display = 'block';
        })
        .catch(error => {
            console.error('Lỗi khi lấy chi tiết hợp đồng:', error);
            alert('Lỗi khi lấy thông tin chi tiết');
        });
}

/**
 * Đóng modal giao xe
 */
function closeDeliverModal() {
    document.getElementById('deliverModal').style.display = 'none';
    currentRentalId = null;
}

/**
 * Xử lý chọn ảnh giao xe
 */
function handleDeliveryPhotoSelect(event) {
    const file = event.target.files[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = function(e) {
        const preview = document.getElementById('deliveryPhotoPreview');
        const previewImg = document.getElementById('deliveryPreviewImg');
        const photoFileName = document.getElementById('deliveryPhotoFileName');

        previewImg.src = e.target.result;
        photoFileName.textContent = file.name;
        preview.style.display = 'block';

        // Store base64 for later use
        window.currentDeliveryPhotoBase64 = e.target.result;
        window.currentDeliveryPhotoFileName = file.name;
    };
    reader.readAsDataURL(file);
}

/**
 * Mở camera để chụp hình giao xe
 */
async function startDeliveryCamera() {
    try {
        const video = document.getElementById('deliveryVideoStream');
        const controls = document.getElementById('deliveryCameraControls');
        const captureControls = document.getElementById('deliveryCaptureControls');

        // Yêu cầu quyền truy cập camera
        const stream = await navigator.mediaDevices.getUserMedia({
            video: { facingMode: 'environment' }
        });

        video.srcObject = stream;
        video.style.display = 'block';
        controls.style.display = 'none';
        captureControls.style.display = 'block';

        // Lưu stream để đóng sau
        window.deliveryMediaStream = stream;

        // Đợi video sẵn sàng (cấp độ phát)
        video.onloadedmetadata = function() {
            video.play().catch(err => console.error('Lỗi play video:', err));
        };

    } catch (error) {
        console.error('Lỗi khi mở camera:', error);
        alert('Không thể mở camera. Vui lòng kiểm tra quyền truy cập.');
    }
}

/**
 * Chụp ảnh từ camera
 */
function captureDeliveryPhoto() {
    const video = document.getElementById('deliveryVideoStream');
    const canvas = document.getElementById('deliveryPhotoCanvas');
    const context = canvas.getContext('2d');

    // Đợi video sẵn sàng (có thể mất vài ms)
    if (video.videoWidth === 0 || video.videoHeight === 0) {
        alert('Vui lòng đợi camera tải xong trước khi chụp');
        return;
    }

    // Set canvas size theo kích thước video
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;

    // Vẽ frame hiện tại từ video vào canvas
    context.drawImage(video, 0, 0);

    // Convert canvas to base64
    const imageData = canvas.toDataURL('image/jpeg', 0.9);

    // Kiểm tra xem ảnh có hợp lệ không (không phải ảnh trắng/đen)
    if (!imageData || imageData.length < 100) {
        alert('Lỗi khi chụp ảnh. Vui lòng thử lại.');
        return;
    }

    // Hiển thị ảnh đã chụp
    const preview = document.getElementById('deliveryPhotoPreview');
    const previewImg = document.getElementById('deliveryPreviewImg');
    const photoFileName = document.getElementById('deliveryPhotoFileName');

    previewImg.src = imageData;
    photoFileName.textContent = 'Ảnh từ camera - ' + new Date().toLocaleTimeString('vi-VN');
    preview.style.display = 'block';

    // Lưu ảnh base64
    window.currentDeliveryPhotoBase64 = imageData;
    window.currentDeliveryPhotoFileName = 'delivery-photo-' + Date.now() + '.jpg';

    // Ẩn video và hiển thị button chụp lại
    video.style.display = 'none';
    document.getElementById('deliveryCaptureControls').style.display = 'none';
    document.getElementById('deliveryRetakeControls').style.display = 'block';

    // Đóng camera
    closeDeliveryCamera();
}

/**
 * Đóng camera
 */
function closeDeliveryCamera() {
    if (window.deliveryMediaStream) {
        window.deliveryMediaStream.getTracks().forEach(track => track.stop());
        window.deliveryMediaStream = null;
    }

    const video = document.getElementById('deliveryVideoStream');
    video.style.display = 'none';
    video.srcObject = null;
}

/**
 * Reset ảnh - chụp lại
 */
function resetDeliveryPhoto() {
    // Xóa ảnh preview
    document.getElementById('deliveryPhotoPreview').style.display = 'none';
    document.getElementById('deliveryPreviewImg').src = '';

    // Reset state
    window.currentDeliveryPhotoBase64 = null;
    window.currentDeliveryPhotoFileName = null;

    // Hiển thị button mở camera
    document.getElementById('deliveryCameraControls').style.display = 'block';
    document.getElementById('deliveryRetakeControls').style.display = 'none';
}

/**
 * Xác nhận giao xe
 * Gửi POST request với returnNotes (ghi chú) và ảnh
 */
function confirmDeliver() {
    if (!currentRentalId) {
        alert('Lỗi: Không tìm thấy thông tin đơn thuê');
        return;
    }

    const note = document.getElementById('deliverNote').value;
    const photoBase64 = window.currentDeliveryPhotoBase64;

    // Xây dựng URL với tham số returnNotes
    let url = `/api/staff/deliver/${currentRentalId}/confirm`;
    if (note && note.trim()) {
        url += `?returnNotes=${encodeURIComponent(note)}`;
    }

    fetch(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        }
    })
    .then(response => {
        if (!response.ok) {
            return response.json().then(data => {
                throw new Error(data.error || `HTTP error! status: ${response.status}`);
            });
        }
        return response.json();
    })
    .then(data => {
        if (data.error) {
            alert('Lỗi: ' + data.error);
        } else {
            // ✅ Gửi ảnh nếu có
            if (photoBase64) {
                saveDeliveryPhoto(currentRentalId, photoBase64);
            }

            // ✅ Hiển thị chi tiết giao xe thành công
            const successMsg = `✓ Xe đã được giao thành công!\n\n` +
                `Trạng thái đơn: ${data.rentalStatus || 'DELIVERED'}\n` +
                `Trạng thái thanh toán: ${formatPaymentStatus(data.paymentStatus) || 'N/A'}\n` +
                `Trạng thái xe: ${data.vehicleStatus || 'RENTED'}`;

            alert(successMsg);
            closeDeliverModal();
            // Tải lại danh sách
            loadDeliveryVehicles();
        }
    })
    .catch(error => {
        console.error('Lỗi khi giao xe:', error);
        alert('Lỗi khi giao xe: ' + error.message);
    });
}

/**
 * Lưu ảnh giao xe vào RentalRecord
 */
function saveDeliveryPhoto(rentalId, photoBase64) {
    try {
        // Convert base64 to binary
        const binaryString = atob(photoBase64.split(',')[1]);
        const bytes = new Uint8Array(binaryString.length);
        for (let i = 0; i < binaryString.length; i++) {
            bytes[i] = binaryString.charCodeAt(i);
        }

        // Gửi binary data lên server
        fetch(`/api/staff/deliver/${rentalId}/photo`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/octet-stream',
                'X-Photo-Name': window.currentDeliveryPhotoFileName || 'delivery-photo'
            },
            body: bytes.buffer
        })
        .then(response => {
            if (response.ok) {
                console.log('Ảnh giao xe đã được lưu thành công');
            } else {
                console.warn('Cảnh báo: Lỗi khi lưu ảnh, nhưng hợp đồng đã được cập nhật');
            }
        })
        .catch(error => {
            console.warn('Cảnh báo: Lỗi khi lưu ảnh:', error);
        });
    } catch (error) {
        console.warn('Cảnh báo: Lỗi xử lý ảnh:', error);
    }
}

/**
 * Đóng modal khi click bên ngoài
 */
window.onclick = function(event) {
    const modal = document.getElementById('deliverModal');
    if (event.target == modal) {
        modal.style.display = 'none';
    }
}

/**
 * Tìm kiếm theo biển số xe (thời gian thực)
 */
document.addEventListener('DOMContentLoaded', function() {
    const searchPlateInput = document.getElementById('searchPlate');
    if (searchPlateInput) {
        searchPlateInput.addEventListener('keyup', function() {
            filterTable();
        });
    }
});

/**
 * Tìm kiếm theo tên khách hàng (thời gian thực)
 */
document.addEventListener('DOMContentLoaded', function() {
    const searchCustomerInput = document.getElementById('searchCustomer');
    if (searchCustomerInput) {
        searchCustomerInput.addEventListener('keyup', function() {
            filterTable();
        });
    }
});

/**
 * Hàm lọc bảng theo cả hai tiêu chí: biển số và tên khách hàng
 * Sử dụng logic AND - cả hai điều kiện phải match
 */
function filterTable() {
    const searchPlate = document.getElementById('searchPlate').value.toLowerCase();
    const searchCustomer = document.getElementById('searchCustomer').value.toLowerCase();
    const tableBody = document.getElementById('deliveryTableBody');
    const rows = tableBody.getElementsByTagName('tr');

    for (let row of rows) {
        const cells = row.querySelectorAll('td');

        if (cells.length >= 3) {
            // Cột 0: Biển số xe
            const plate = cells[0].textContent.toLowerCase();
            // Cột 2: Tên khách hàng (username từ RentalRecord)
            const customerName = cells[2].textContent.toLowerCase();

            const matchPlate = plate.includes(searchPlate);
            const matchCustomer = customerName.includes(searchCustomer);

            // Hiển thị dòng nếu cả hai điều kiện đều match (AND logic)
            // Hoặc chỉ một nếu chỉ có một field được nhập
            if ((searchPlate === '' || matchPlate) && (searchCustomer === '' || matchCustomer)) {
                row.style.display = '';
            } else {
                row.style.display = 'none';
            }
        }
    }
}

