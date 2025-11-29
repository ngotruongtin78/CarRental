// ========== STAFF RETURN VEHICLE PAGE SCRIPT ==========

// Lưu thông tin rental hiện tại
let currentRentalId = null;

// Tải danh sách xe sẵn sàng trả khi trang load
document.addEventListener('DOMContentLoaded', function() {
    loadReturnVehicles();
});

/**
 * Lấy danh sách các xe sẵn sàng trả từ API
 * Điều kiện: status = "WAITING_INSPECTION" (xe đã được giao cho khách)
 */
function loadReturnVehicles() {
    fetch('/api/staff/return/vehicles-ready')
        .then(response => response.json())
        .then(data => {
            populateReturnTable(data);
        })
        .catch(error => {
            console.error('Lỗi khi tải danh sách xe:', error);
            alert('Lỗi khi tải danh sách xe sẵn sàng trả');
        });
}

/**
 * Điền dữ liệu vào bảng danh sách trả xe
 * Hiển thị: Biển số xe, Loại xe, Tên người thuê, Ngày thuê, Trạng thái thanh toán
 */
function populateReturnTable(rentalRecords) {
    const tableBody = document.getElementById('returnTableBody');
    tableBody.innerHTML = '';

    if (!rentalRecords || rentalRecords.length === 0) {
        tableBody.innerHTML = '<tr><td colspan="6" style="text-align: center; padding: 20px; color: #999;">Không có đơn hàng sẵn sàng trả</td></tr>';
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
                <button class="btn-action btn-deliver-now" onclick="handleReturnVehicle('${rental.id}', '${vehiclePlate}', '${customerName}')">
                    <span class="icon">↩️</span>
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

function formatDateTime(value) {
    if (!value) return '';
    const date = new Date(value);
    if (isNaN(date)) return '';
    return `${date.toLocaleDateString()} ${date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`;
}

function formatLocation(lat, lng) {
    if (lat === null || lng === null || lat === undefined || lng === undefined) return 'N/A';
    const latNum = Number(lat);
    const lngNum = Number(lng);
    if (Number.isNaN(latNum) || Number.isNaN(lngNum)) return 'N/A';
    return `${latNum.toFixed(5)}, ${lngNum.toFixed(5)}`;
}

/**
 * Mở modal trả xe với toàn bộ thông tin chi tiết
 */
function handleReturnVehicle(rentalId, plate, customerName) {
    currentRentalId = rentalId;

    // Gọi API để lấy chi tiết hợp đồng
    fetch(`/api/staff/return/${rentalId}`)
        .then(response => response.json())
        .then(data => {
            // Điền thông tin xe
            document.getElementById('returnPlate').value = data.vehiclePlate || plate;
            document.getElementById('returnVehicleType').value = data.vehicleType || 'N/A';

            // Điền thông tin khách hàng
            document.getElementById('returnCustomer').value = data.username || customerName;

            // Điền thông tin thuê xe
            document.getElementById('returnStartDate').value = formatDate(data.startDate) || 'N/A';
            document.getElementById('returnEndDate').value = formatDate(data.endDate) || 'N/A';
            document.getElementById('returnRentalDays').value = (data.rentalDays || 0) + ' ngày';

            // Điền thông tin thanh toán
            document.getElementById('returnTotal').value = formatCurrency(data.total) || 'N/A';
            document.getElementById('returnPaymentStatus').value = formatPaymentStatus(data.paymentStatus) || 'N/A';

            document.getElementById('returnCheckinTime').value = formatDateTime(data.checkinTime) || 'N/A';
            document.getElementById('returnCheckinLoc').value = formatLocation(data.checkinLatitude, data.checkinLongitude);
            document.getElementById('returnRequestTime').value = formatDateTime(data.returnTime) || 'N/A';
            document.getElementById('returnRequestLoc').value = formatLocation(data.returnLatitude, data.returnLongitude);
            document.getElementById('returnCustomerNote').value = data.returnNotes || 'Không có';

            // Hiển thị ảnh xe trước khi giao (deliveryPhotoData)
            if (data.deliveryPhotoData) {
                // deliveryPhotoData là base64 hoặc binary, nếu là binary cần convert
                let photoSrc = data.deliveryPhotoData;

                // Nếu không phải data URL, thêm prefix
                if (!photoSrc.startsWith('data:')) {
                    // Giả sử deliveryPhotoData là string hoặc blob
                    if (typeof photoSrc === 'string') {
                        photoSrc = 'data:image/png;base64,' + photoSrc;
                    }
                }

                document.getElementById('deliveryPhotoPreviewBox').style.display = 'block';
                document.getElementById('noDeliveryPhotoBox').style.display = 'none';
                document.getElementById('deliveryPhotoPreviewImg').src = photoSrc;
            } else {
                document.getElementById('deliveryPhotoPreviewBox').style.display = 'none';
                document.getElementById('noDeliveryPhotoBox').style.display = 'block';
            }

            // Làm trống phí phát sinh và ghi chú
            document.getElementById('returnDamageFee').value = '';
            document.getElementById('returnNote').value = '';

            // Reset camera state
            closeReturnCamera();
            document.getElementById('returnVideoStream').style.display = 'none';
            document.getElementById('returnPhotoPreview').style.display = 'none';
            document.getElementById('returnCameraControls').style.display = 'block';
            document.getElementById('returnCaptureControls').style.display = 'none';
            document.getElementById('returnRetakeControls').style.display = 'none';
            window.currentReturnPhotoBase64 = null;
            window.currentReturnPhotoFileName = null;
            window.currentReturnPhotoTimestamp = null;

            // Mở modal
            document.getElementById('returnModal').style.display = 'block';
        })
        .catch(error => {
            console.error('Lỗi khi lấy chi tiết hợp đồng:', error);
            alert('Lỗi khi lấy thông tin chi tiết');
        });
}

/**
 * Đóng modal trả xe
 */
function closeReturnModal() {
    document.getElementById('returnModal').style.display = 'none';
    currentRentalId = null;
}

/**
 * Xác nhận trả xe
 * Gửi POST request với damageFee (phí phát sinh) và returnNote
 */
function confirmReturn() {
    if (!currentRentalId) {
        alert('Lỗi: Không tìm thấy thông tin đơn thuê');
        return;
    }

    const damageFee = document.getElementById('returnDamageFee').value || '0';
    const note = document.getElementById('returnNote').value;

    // Xây dựng URL với tham số damageFee (phí phát sinh) và returnNote
    let url = `/api/staff/return/${currentRentalId}/confirm?damageFee=${damageFee}`;
    if (note && note.trim()) {
        url += `&returnNote=${encodeURIComponent(note)}`;
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
            // ✅ Gửi ảnh nếu có (await để đảm bảo hoàn thành)
            const photoBase64 = window.currentReturnPhotoBase64;
            if (photoBase64) {
                saveReturnPhoto(currentRentalId, photoBase64);
            }

            // ✅ Hiển thị chi tiết trả xe thành công
            const successMsg = `✓ Xe đã được trả thành công!\n\n` +
                `Trạng thái đơn: ${data.rentalStatus || 'COMPLETED'}\n` +
                `Trạng thái thanh toán: ${formatPaymentStatus(data.paymentStatus) || 'N/A'}\n` +
                `Trạng thái xe: ${data.vehicleStatus || 'AVAILABLE'}`;

            alert(successMsg);
            closeReturnModal();
            // Tải lại danh sách
            loadReturnVehicles();
        }
    })
    .catch(error => {
        alert('Lỗi khi trả xe: ' + error.message);
    });
}

/**
 * Đóng modal khi click bên ngoài
 */
window.onclick = function(event) {
    const modal = document.getElementById('returnModal');
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
    const tableBody = document.getElementById('returnTableBody');
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

/**
 * Mở camera để chụp hình trả xe
 */
async function startReturnCamera() {
    try {
        const video = document.getElementById('returnVideoStream');
        const controls = document.getElementById('returnCameraControls');
        const captureControls = document.getElementById('returnCaptureControls');

        // Yêu cầu quyền truy cập camera
        const stream = await navigator.mediaDevices.getUserMedia({
            video: { facingMode: 'environment' }
        });

        video.srcObject = stream;
        video.style.display = 'block';
        controls.style.display = 'none';
        captureControls.style.display = 'block';

        // Lưu stream để đóng sau
        window.returnMediaStream = stream;

        // Đợi video sẵn sàng
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
function captureReturnPhoto() {
    const video = document.getElementById('returnVideoStream');
    const canvas = document.getElementById('returnPhotoCanvas');
    const context = canvas.getContext('2d');

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

    if (!imageData || imageData.length < 100) {
        alert('Lỗi khi chụp ảnh. Vui lòng thử lại.');
        return;
    }

    // Hiển thị ảnh đã chụp
    const preview = document.getElementById('returnPhotoPreview');
    const previewImg = document.getElementById('returnPreviewImg');
    const photoFileName = document.getElementById('returnPhotoFileName');

    previewImg.src = imageData;
    
    // Lưu timestamp khi chụp ảnh để hiển thị và lưu cùng ảnh
    const captureTimestamp = new Date();
    const formattedDateTime = captureTimestamp.toLocaleString('vi-VN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
    });
    photoFileName.textContent = 'Ảnh trả xe - ' + formattedDateTime;
    preview.style.display = 'block';

    // Lưu ảnh base64 và timestamp
    window.currentReturnPhotoBase64 = imageData;
    window.currentReturnPhotoFileName = 'return-photo-' + captureTimestamp.getTime() + '.jpg';
    // Lưu timestamp vào biến toàn cục để gửi lên server (ISO format)
    window.currentReturnPhotoTimestamp = captureTimestamp.toISOString();

    // Ẩn video và hiển thị button chụp lại
    video.style.display = 'none';
    document.getElementById('returnCaptureControls').style.display = 'none';
    document.getElementById('returnRetakeControls').style.display = 'block';

    // Đóng camera
    closeReturnCamera();
}

/**
 * Đóng camera
 */
function closeReturnCamera() {
    if (window.returnMediaStream) {
        window.returnMediaStream.getTracks().forEach(track => track.stop());
        window.returnMediaStream = null;
    }

    const video = document.getElementById('returnVideoStream');
    video.style.display = 'none';
    video.srcObject = null;
}

/**
 * Reset ảnh - chụp lại
 */
function resetReturnPhoto() {
    // Xóa ảnh preview
    document.getElementById('returnPhotoPreview').style.display = 'none';
    document.getElementById('returnPreviewImg').src = '';

    // Reset state
    window.currentReturnPhotoBase64 = null;
    window.currentReturnPhotoFileName = null;
    window.currentReturnPhotoTimestamp = null;

    // Hiển thị button mở camera
    document.getElementById('returnCameraControls').style.display = 'block';
    document.getElementById('returnRetakeControls').style.display = 'none';
}

/**
 * Lưu ảnh trả xe vào RentalRecord (receivePhotoData)
 */
async function saveReturnPhoto(rentalId, photoBase64) {
    try {
        if (!photoBase64 || photoBase64.length < 100) {
            return;
        }

        // Convert base64 to binary
        const binaryString = atob(photoBase64.split(',')[1]);
        const bytes = new Uint8Array(binaryString.length);
        for (let i = 0; i < binaryString.length; i++) {
            bytes[i] = binaryString.charCodeAt(i);
        }

        // Gửi binary data lên server
        const response = await fetch(`/api/staff/return/${rentalId}/receive-photo`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/octet-stream',
                'X-Photo-Name': window.currentReturnPhotoFileName || 'receive-photo'
            },
            body: bytes.buffer
        });

        if (!response.ok) {
            console.error('❌ Lỗi khi lưu ảnh');
        }
    } catch (error) {
        console.error('❌ Lỗi xử lý ảnh:', error);
    }
}
