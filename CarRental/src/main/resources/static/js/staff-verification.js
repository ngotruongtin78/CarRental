document.addEventListener("DOMContentLoaded", function() {

    const verificationListBody = document.getElementById('verification-list-body');
    const approveBtn = document.getElementById('approve-btn');
    const denyBtn = document.getElementById('deny-btn');

    // Tham chiếu đến khu vực xem ảnh
    const docPreviewFront = document.getElementById('doc-preview-front');
    const docPreviewBack = document.getElementById('doc-preview-back');
    const docInfoText = document.getElementById('doc-info-text');

    let selectedUserId = null;
    loadVerificationRequests();

    async function loadVerificationRequests() {
        try {
            // =================================================================
            // BACKEND CALL: GET /api/staff/verifications/pending (Giả định)
            // Mục đích: Lấy danh sách Users có (verified = false VÀ (licenseData != null HOẶC idCardData != null)).
            //
            // API này cần được tạo mới ở Backend.
            //
            // Backend trả về:
            // [
            //   {
            //     "userId": "user_1", "fullName": "Nguyễn Văn A", "email": "nvane@email.com",
            //     "docType": "Giấy phép lái xe", "submittedAt": "10 phút trước"
            //   },
            //   {
            //     "userId": "user_1", "fullName": "Nguyễn Văn A", "email": "nvane@email.com",
            //     "docType": "CMND/CCCD", "submittedAt": "10 phút trước"
            //   }
            // ]
            // =================================================================

            // --- GIẢ LẬP DỮ LIỆU ---
            const fakeData = [
                { "userId": "user_1", "fullName": "Nguyễn Văn A", "email": "@speeblisJXidieu.faom", "docType": "Giấy phép lái xe", "submittedAt": "10 phút trước" },
                { "userId": "user_2", "fullName": "Nguyễn Văn B", "email": "@speeblisJXidieu.faom", "docType": "CMND/CCCD", "submittedAt": "1 giờ trước" },
                { "userId": "user_3", "fullName": "Lê Thị C", "email": "@HondaA.VPS34", "docType": "Giấy phép lái xe", "submittedAt": "2 giờ trước" }
            ];
            // --- KẾT THÚC GIẢ LẬP ---

            verificationListBody.innerHTML = '';
            const requests = fakeData;
            requests.forEach(req => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td>
                        <img src="/images/avatar-placeholder.png" class="user-avatar" alt="avatar">
                        <div>
                            <span class="user-name">${req.fullName}</span>
                            <span class="user-email">${req.email}</span>
                        </div>
                    </td>
                    <td><span class="status-pending-orange">Chờ xác nhận</span></td>
                    <td>${req.docType}</td>
                    <td>${req.submittedAt}</td>
                    <td><a href="#" class="action-link" data-userid="${req.userId}">Nhấn để xem</a></td>
                `;
                verificationListBody.appendChild(tr);
            });

        } catch (error) {
            console.error("Lỗi tải danh sách chờ:", error);
            verificationListBody.innerHTML = '<tr><td colspan="5">Lỗi khi tải dữ liệu.</td></tr>';
        }
    }
    verificationListBody.addEventListener('click', function(event) {
        if (event.target.classList.contains('action-link')) {
            event.preventDefault();
            selectedUserId = event.target.dataset.userid;

            alert(`(Giả lập) Đang tải thông tin của User ID: ${selectedUserId}`);

            // =================================================================
            // BACKEND CALL: GET /api/staff/verifications/detail/{userId} (Giả định)
            // Mục đích: Tải chi tiết thông tin và ảnh (dưới dạng Base64 hoặc URL) của user đó.
            // API này sẽ lấy `licenseData` và `idCardData` từ User.
            // =================================================================

            // Giả lập hiển thị dữ liệu
            docPreviewFront.innerHTML = `<img src="/images/id-card-front.jpg" style="width:100%; height:100%; object-fit: cover;">`;
            docPreviewBack.innerHTML = `<img src="/images/id-card-back.jpg" style="width:100%; height:100%; object-fit: cover;">`;
            docInfoText.innerHTML = `
                <strong>Họ tên:</strong> Nguyễn Văn A<br>
                <strong>Số CMND:</strong> 0123456789<br>
                <strong>Ngày sinh:</strong> 01/01/1990<br>
                <strong>Nơi cấp:</strong> Cục Cảnh sát...
            `;
        }
    });
    approveBtn.addEventListener('click', () => handleVerification(true));
    denyBtn.addEventListener('click', () => handleVerification(false));

    async function handleVerification(isApproved) {
        if (!selectedUserId) {
            alert('Vui lòng chọn một khách hàng từ danh sách chờ.');
            return;
        }

        const action = isApproved ? 'Chấp thuận' : 'Từ chối';

        try {
            // =================================================================
            // BACKEND CALL: POST /api/staff/verifications/process (Giả định)
            // Mục đích: Cập nhật trạng thái 'verified' cho User.
            // API này sẽ nhận 1 JSON: { "userId": "...", "approved": true/false }
            // Nếu approved = true, API set 'user.verified = true'.
            // =================================================================

            /*
            const response = await fetch('/api/staff/verifications/process', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ userId: selectedUserId, approved: isApproved })
            });

            if (response.ok) {
                alert(`Đã ${action} thành công!`);
                loadVerificationRequests(); // Tải lại danh sách
                // Xóa thông tin ở khu vực xem
                docPreviewFront.innerHTML = '(chưa chọn)';
                docPreviewBack.innerHTML = '(chưa chọn)';
                docInfoText.innerHTML = '';
                selectedUserId = null;
            } else {
                alert(`Có lỗi khi ${action}.`);
            }
            */

            alert(`(Giả lập) Đã ${action} cho User ID: ${selectedUserId}`);
            loadVerificationRequests();
        } catch (error) {
            console.error(`Lỗi khi ${action}:`, error);
        }
    }

});