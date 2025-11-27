// ========== STAFF DELIVER PAGE SCRIPT ==========

// Lưu thông tin rental hiện tại
let currentRentalId = null;

// Signature pad instance
let signaturePad = null;

// Debug mode
const DEBUG = true;

function log(message, data = null) {
    if (DEBUG) {
        if (data) {
            console.log(`🔵 [DELIVER] ${message}`, data);
        } else {
            console.log(`🔵 [DELIVER] ${message}`);
        }
    }
}

// Tải danh sách xe sẵn sàng giao khi trang load
document.addEventListener('DOMContentLoaded', function() {
    log('🎯 DOMContentLoaded - Trang đã load xong');
    log('SignaturePad library:', typeof SignaturePad !== 'undefined' ? 'CÓ ✓' : 'KHÔNG ✗');

    loadDeliveryVehicles();
});

/**
 * Lấy danh sách các xe sẵn sàng giao từ API
 * Điều kiện: paymentStatus = "PAID" hoặc "DEPOSIT_PENDING"
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
        'DEPOSIT_PENDING': 'Thanh toán tại trạm',
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

            // Xử lý paymentStatus
            let paymentStatusDisplay = formatPaymentStatus(data.paymentStatus) || 'N/A';

            // Nếu paymentStatus = PAY_AT_STATION, hiển thị "Đã đặt cọc"
            if (data.paymentStatus === 'PAY_AT_STATION') {
                paymentStatusDisplay = '✅ Đã đặt cọc';

                // Hiển thị section tiền đặt cọc
                const depositSection = document.getElementById('depositInfoSection');
                if (depositSection) {
                    depositSection.style.display = 'block';

                    // Tính tiền đặt cọc và tiền còn lại
                    const depositPaid = data.depositPaidAmount || 0;
                    const remaining = (data.total || 0) - depositPaid;

                    document.getElementById('deliverDepositPaidAmount').value = formatCurrency(depositPaid) || '0 ₫';
                    document.getElementById('deliverRemainingAmount').value = formatCurrency(remaining) || '0 ₫';
                }
            } else {
                // Ẩn section tiền đặt cọc nếu không phải PAY_AT_STATION
                const depositSection = document.getElementById('depositInfoSection');
                if (depositSection) {
                    depositSection.style.display = 'none';
                }
            }

            document.getElementById('deliverPaymentStatus').value = paymentStatusDisplay;

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

            // Reset signature
            clearSignature();
            window.currentDeliverySignatureData = null;
            window.currentDeliverySignatureBase64 = null;

            // Mở modal
            document.getElementById('deliverModal').style.display = 'block';

            // ✨ Khởi tạo signature pad sau khi modal mở (canvas đã được render)
            setTimeout(function() {
                initializeSignaturePad();
            }, 100);
        })
        .catch(error => {
            console.error('Lỗi khi lấy chi tiết hợp đồng:', error);
            alert('Lỗi khi lấy thông tin chi tiết');
        });
}

/**
 * Khởi tạo Signature Pad (gọi khi modal mở)
 */
function initializeSignaturePad() {
    log('🔴 initializeSignaturePad() CALLED');

    const canvas = document.getElementById('signaturePad');
    log('Canvas element found:', canvas ? 'CÓ ✓' : 'KHÔNG ✗', canvas);

    if (!canvas) {
        console.error('❌ Canvas signaturePad không tìm thấy');
        return;
    }

    // Hủy instance cũ nếu có
    if (signaturePad) {
        log('Clearing old SignaturePad instance');
        signaturePad.clear();
    }

    // Kiểm tra library
    if (typeof SignaturePad === 'undefined') {
        console.error('❌ SignaturePad library không load');
        return;
    }

    // Tạo instance mới
    try {
        signaturePad = new SignaturePad(canvas, {
            backgroundColor: 'rgb(255, 255, 255)',
            penColor: 'rgb(255, 68, 68)', // Màu đỏ (như màu đèn) - dễ nhìn hơn
            dotSize: 3,
            minWidth: 1,
            maxWidth: 3,
            throttle: 16,
            minDistance: 5
        });

        log('✅ Signature Pad initialized successfully:', {
            type: signaturePad.constructor.name,
            penColor: 'rgb(255, 68, 68)',
            canvasSize: `${canvas.width}x${canvas.height}`
        });

        // Resize canvas để phù hợp với container
        resizeSignaturePad();

        // Test: vẽ một đường test
        log('Canvas ready for drawing');

    } catch (error) {
        console.error('❌ Lỗi khi khởi tạo SignaturePad:', error);
        log('Error details:', error.message);
    }
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
 * Gửi POST request với returnNotes (ghi chú), ảnh, và chữ ký
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

            // ✅ Gửi chữ ký nếu có
            const signatureData = getSignatureData();
            if (signatureData) {
                saveDeliverySignature(currentRentalId, signatureData.imageData);
            }

            // ✅ Hiển thị chi tiết giao xe thành công
            const successMsg = `✓ Xe đã được giao thành công!\n\n` +
                `Trạng thái đơn: ${data.rentalStatus || 'WAITING_INSPECTION'}\n` +
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
 * Lưu chữ ký giao xe vào RentalRecord
 */
function saveDeliverySignature(rentalId, signatureBase64) {
    try {
        // Convert base64 to binary
        const binaryString = atob(signatureBase64.split(',')[1]);
        const bytes = new Uint8Array(binaryString.length);
        for (let i = 0; i < binaryString.length; i++) {
            bytes[i] = binaryString.charCodeAt(i);
        }

        // Gửi binary data lên server
        fetch(`/api/staff/deliver/${rentalId}/signature`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/octet-stream',
                'X-Signature-Name': 'delivery-signature'
            },
            body: bytes.buffer
        })
        .then(response => {
            if (response.ok) {
                console.log('Chữ ký giao xe đã được lưu thành công');
            } else {
                console.warn('Cảnh báo: Lỗi khi lưu chữ ký, nhưng hợp đồng đã được cập nhật');
            }
        })
        .catch(error => {
            console.warn('Cảnh báo: Lỗi khi lưu chữ ký:', error);
        });
    } catch (error) {
        console.warn('Cảnh báo: Lỗi xử lý chữ ký:', error);
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

/**
 * Resize signature pad khi cửa sổ thay đổi kích thước
 */
function resizeSignaturePad() {
    const canvas = document.getElementById('signaturePad');
    if (!canvas) {
        log('❌ Canvas không tìm thấy');
        return;
    }

    if (!signaturePad) {
        log('⚠️ SignaturePad instance không tồn tại');
        return;
    }

    const container = canvas.parentElement;
    if (!container) {
        log('❌ Container không tồn tại');
        return;
    }

    const ratio = Math.max(window.devicePixelRatio || 1, 1);

    // Lấy kích thước thực tế của container
    const width = container.offsetWidth;
    const height = 200;

    log('Resizing canvas:', { width, height, ratio });

    // Set canvas size
    canvas.width = width * ratio;
    canvas.height = height * ratio;

    // Scale context
    canvas.getContext('2d').scale(ratio, ratio);

    // Set display size
    canvas.style.width = width + 'px';
    canvas.style.height = height + 'px';

    log('✅ Canvas resized to:', `${width}x${height} (ratio: ${ratio})`);

    // Vẽ background trắng
    const ctx = canvas.getContext('2d');
    ctx.fillStyle = 'rgb(255, 255, 255)';
    ctx.fillRect(0, 0, width, height);

    // Khôi phục chữ ký nếu có
    if (window.currentDeliverySignatureData) {
        log('Restoring previous signature data');
        signaturePad.fromData(window.currentDeliverySignatureData);
    }
}

/**
 * Xóa chữ ký
 */
function clearSignature() {
    if (signaturePad) {
        signaturePad.clear();
        window.currentDeliverySignatureData = null;
        window.currentDeliverySignatureBase64 = null;
        updateSignatureStatus('Chữ ký đã bị xóa');
        console.log('✓ Signature cleared');
        setTimeout(() => {
            const statusEl = document.getElementById('signatureStatus');
            if (statusEl) {
                statusEl.textContent = '';
            }
        }, 2000);
    } else {
        console.warn('⚠️ SignaturePad instance không tồn tại');
    }
}

/**
 * Cập nhật trạng thái chữ ký
 */
function updateSignatureStatus(message) {
    const statusEl = document.getElementById('signatureStatus');
    if (statusEl) {
        statusEl.textContent = message;
        statusEl.style.color = '#27ae60';
    }
}

/**
 * Lấy chữ ký dưới dạng base64
 */
function getSignatureData() {
    if (!signaturePad) {
        console.warn('⚠️ SignaturePad instance không tồn tại');
        return null;
    }

    if (signaturePad.isEmpty()) {
        console.warn('⚠️ Canvas chữ ký trống - không có chữ ký nào');
        return null;
    }

    try {
        // Lưu dữ liệu signature
        const signatureData = signaturePad.toData();
        window.currentDeliverySignatureData = signatureData;

        // Lấy ảnh base64
        const imageData = signaturePad.toDataURL('image/png');
        window.currentDeliverySignatureBase64 = imageData;

        console.log('✓ Signature data captured:', {
            dataPoints: signatureData.length,
            imageSize: imageData.length
        });

        return {
            data: signatureData,
            imageData: imageData
        };
    } catch (error) {
        console.error('❌ Lỗi khi lấy chữ ký:', error);
        return null;
    }
}
