document.addEventListener("DOMContentLoaded", function() {

    const canvas = document.getElementById('signature-pad');
    let signaturePad;
    try {
        signaturePad = new SignaturePad(canvas, {
            backgroundColor: 'rgb(249, 249, 249)',
            penColor: 'rgb(0, 0, 0)'
        });
    } catch (e) {
        console.error("Không thể khởi tạo SignaturePad", e);
        document.querySelector('section:nth-of-type(3)').style.display = 'none';
    }

    const clearButton = document.getElementById('clear-signature');
    if (clearButton) {
        clearButton.addEventListener('click', function () {
            if(signaturePad) signaturePad.clear();
        });
    }
    const takePhotoButton = document.getElementById('take-photo-btn');
    const photoInput = document.getElementById('photo-input');
    const photoPreviewContainer = document.getElementById('photo-preview-container');

    if (takePhotoButton && photoInput) {
        takePhotoButton.addEventListener('click', () => {
            photoInput.click();
        });

        photoInput.addEventListener('change', (event) => {
            photoPreviewContainer.innerHTML = '';
            if (event.target.files) {
                Array.from(event.target.files).forEach(file => {
                    const reader = new FileReader();
                    reader.onload = (e) => {
                        const img = document.createElement('img');
                        img.src = e.target.result;
                        img.style.width = '120px';
                        img.style.height = '90px';
                        img.style.objectFit = 'cover';
                        img.style.borderRadius = '6px';
                        photoPreviewContainer.appendChild(img);
                    };
                    reader.readAsDataURL(file);
                });
            }
        });
    }
    const rentalFeeEl = document.getElementById('rental-fee');
    const damageFeeEl = document.getElementById('damage-fee');
    const totalFeeEl = document.getElementById('total-fee');
    const finalPaymentEl = document.getElementById('final-payment');
    function parseCurrency(text) {
        return parseFloat(text.replace(/[.đ]/g, '')) || 0;
    }
    function formatCurrency(number) {
        return number.toLocaleString('vi-VN') + 'đ';
    }

    function calculateTotal() {
        const rentalFee = parseCurrency(rentalFeeEl.value);
        const damageFee = parseFloat(damageFeeEl.value) || 0;

        const total = rentalFee + damageFee;

        totalFeeEl.value = formatCurrency(total);
        finalPaymentEl.textContent = `KHÁCH CẦN THANH TOÁN: ${formatCurrency(total)}`;
    }

    if(damageFeeEl) damageFeeEl.addEventListener('input', calculateTotal);
    const returnForm = document.getElementById('return-form');
    if (returnForm) {
        returnForm.addEventListener('submit', async function(event) {
            event.preventDefault(); // Ngăn gửi form

            if (signaturePad && signaturePad.isEmpty()) {
                alert('Vui lòng yêu cầu khách hàng và nhân viên ký xác nhận.');
                return;
            }
            const formData = new FormData();
            const rentalId = "RENTAL_ID_EXAMPLE"; // Backend cần truyền ID này

            formData.append('rentalId', rentalId);
            formData.append('damageFee', parseFloat(damageFeeEl.value) || 0);
            formData.append('notes', document.getElementById('incident-code').value);
            formData.append('hasDamage', document.getElementById('has-damage-toggle').checked);
            if (signaturePad) {
                const signatureImage = signaturePad.toDataURL();
                formData.append('signatureImage', signatureImage);
            }
            const files = photoInput.files;
            if (files.length > 0) {
                for (let i = 0; i < files.length; i++) {
                    formData.append('returnImages', files[i]);
                }
            }

            // =================================================================
            // BACKEND CALL: POST /api/staff/return/{rentalId} (Giả định)
            // Mục đích: Hoàn tất quy trình trả xe.
            // API này cần được tạo mới ở Backend (ví dụ: trong StaffController.java)
            // Nó sẽ nhận:
            // 1. Phí bồi thường, ghi chú, trạng thái hư hỏng.
            // 2. Ảnh chữ ký (Base64) (Lưu vào checkoutPhoto).
            // 3. Danh sách ảnh chụp (MultipartFile).
            // 4. Cập nhật trạng thái xe (Vehicle) thành `available = true`.
            // 5. Cập nhật trạng thái `Rental` thành `RETURNED`.
            // =================================================================

            console.log('Đang gửi dữ liệu...');
            for (let [key, value] of formData.entries()) {
                console.log(key, value);
            }

            alert('(Giả lập) Đã gửi dữ liệu trả xe lên server!');

            /*
            try {
                const response = await fetch(`/api/staff/return/${rentalId}`, {
                    method: 'POST',
                    body: formData // Gửi FormData, không cần set Content-Type
                });

                if (response.ok) {
                    alert('Hoàn tất nhận xe thành công!');
                    window.location.href = '/staff/dashboard'; // Về trang chủ staff
                } else {
                    alert('Có lỗi xảy ra, không thể hoàn tất.');
                }
            } catch (error) {
                console.error('Lỗi gửi form:', error);
                alert('Lỗi kết nối.');
            }
            */
        });
    }

});