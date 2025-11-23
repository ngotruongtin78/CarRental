document.addEventListener("DOMContentLoaded", function() {

    const verificationListBody = document.getElementById('verification-list-body');
    const approveBtn = document.getElementById('approve-btn');
    const denyBtn = document.getElementById('deny-btn');

    // Tham chiếu đến khu vực xem ảnh
    const docPreviewFront = document.getElementById('doc-preview-front');
    const docPreviewBack = document.getElementById('doc-preview-back');
    const docInfoText = document.getElementById('doc-info-text');
    const imageModal = document.getElementById('imageModal');
    const modalImage = document.getElementById('modalImage');
    const imageModalClose = document.querySelector('.image-modal-close');

    let selectedUserId = null;

    // Event listener cho modal
    imageModalClose.addEventListener('click', () => {
        imageModal.style.display = 'none';
    });

    imageModal.addEventListener('click', (e) => {
        if (e.target === imageModal) {
            imageModal.style.display = 'none';
        }
    });

    // Keyboard event để đóng modal
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && imageModal.style.display === 'flex') {
            imageModal.style.display = 'none';
        }
    });

    loadVerificationRequests();

    async function loadVerificationRequests() {
        try {
            // Gọi API backend để lấy danh sách người dùng chờ xác thực
            // API sẽ trả về danh sách người dùng (role = ROLE_USER) chưa xác thực (verified = false)
            // có licenseData hoặc idCardData, sắp xếp theo thời gian mới nhất
            const response = await fetch('/api/staff/verifications/pending', {
                method: 'GET',
                headers: { 'Content-Type': 'application/json' }
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            verificationListBody.innerHTML = '';
            const requests = await response.json();

            if (!requests || requests.length === 0) {
                verificationListBody.innerHTML = '<tr><td colspan="5" style="text-align: center; padding: 20px;">Không có yêu cầu xác thực nào.</td></tr>';
                return;
            }

            // Hiển thị danh sách người dùng chờ xác thực
            requests.forEach(req => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td>
                        <span class="user-name">${req.username || 'N/A'}</span>
                    </td>
                    <td><span class="status-pending-orange">Chờ xác nhận</span></td>
                    <td>${req.docType}</td>
                    <td>${req.submittedAt || '-'}</td>
                    <td><a href="#" class="action-link" data-userid="${req.userId}">Nhấn để xem</a></td>
                `;
                tr.dataset.hasLicense = req.hasLicense || false;
                tr.dataset.hasIdCard = req.hasIdCard || false;
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
            const row = event.target.closest('tr');
            selectedUserId = event.target.dataset.userid;

            const hasLicense = row.dataset.hasLicense === 'true';
            const hasIdCard = row.dataset.hasIdCard === 'true';

            // Nếu không có giấy tờ nào, thông báo
            if (!hasLicense && !hasIdCard) {
                alert('Chưa có giấy tờ nào được nộp từ người dùng này.');
                docPreviewFront.innerHTML = '<span style="color: #999;">(Chưa nộp)</span>';
                docPreviewBack.innerHTML = '<span style="color: #999;">(Chưa nộp)</span>';
                docInfoText.innerHTML = '';
                return;
            }

            // Gọi API backend để lấy chi tiết thông tin người dùng
            fetch(`/api/staff/verifications/detail/${selectedUserId}`, {
                method: 'GET',
                headers: { 'Content-Type': 'application/json' }
            })
            .then(response => response.json())
            .then(data => {
                if (data.error) {
                    alert('Không tìm thấy thông tin người dùng.');
                    return;
                }

                // Hiển thị ảnh giấy tờ từ Base64
                if (hasIdCard && data.idCardData) {
                    // Hiển thị CMND/CCCD ở mặt trước
                    docPreviewFront.innerHTML = `<img src="${data.idCardData}" style="width:100%; height:100%; object-fit: cover; cursor: pointer;" alt="ID Card Front">`;

                    // Nếu có cả licenseData, hiển thị ở mặt sau
                    if (hasLicense && data.licenseData) {
                        docPreviewBack.innerHTML = `<img src="${data.licenseData}" style="width:100%; height:100%; object-fit: cover;" alt="License">`;
                    } else {
                        docPreviewBack.innerHTML = `<span style="color: #999;">-</span>`;
                    }
                } else if (hasLicense && data.licenseData) {
                    // Hiển thị Giấy phép lái xe
                    docPreviewFront.innerHTML = `<img src="${data.licenseData}" style="width:100%; height:100%; object-fit: cover; cursor: pointer;" alt="License">`;
                    docPreviewBack.innerHTML = `<span style="color: #999;">-</span>`;
                } else {
                    docPreviewFront.innerHTML = `<span style="color: #999;">(Không có dữ liệu)</span>`;
                    docPreviewBack.innerHTML = `<span style="color: #999;">-</span>`;
                }

                // Hiển thị thông tin người dùng
                docInfoText.innerHTML = `
                    <strong>Tên đăng nhập:</strong> ${data.username || 'N/A'}<br>
                `;
            })
            .catch(error => {
                console.error("Lỗi khi lấy chi tiết:", error);
                alert('Lỗi khi tải thông tin.');
            });
        }
    });
    approveBtn.addEventListener('click', () => handleVerification(true));
    denyBtn.addEventListener('click', () => handleVerification(false));

    // Event listener để click vào ảnh và phóng to
    docPreviewFront.addEventListener('click', function(e) {
        if (e.target.tagName === 'IMG') {
            modalImage.src = e.target.src;
            imageModal.style.display = 'flex';
        }
    });

    docPreviewBack.addEventListener('click', function(e) {
        if (e.target.tagName === 'IMG') {
            modalImage.src = e.target.src;
            imageModal.style.display = 'flex';
        }
    });

    async function handleVerification(isApproved) {
        if (!selectedUserId) {
            alert('Vui lòng chọn một khách hàng từ danh sách chờ.');
            return;
        }

        const action = isApproved ? 'Chấp thuận' : 'Từ chối';

        try {
            // Gọi API backend để xác thực hoặc từ chối người dùng
            const response = await fetch('/api/staff/verifications/process', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ userId: selectedUserId, approved: isApproved })
            });

            const result = await response.json();

            if (response.ok && (result.status === 'APPROVED' || result.status === 'DENIED')) {
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
        } catch (error) {
            console.error(`Lỗi khi ${action}:`, error);
            alert(`Lỗi khi ${action}. Vui lòng thử lại.`);
        }
    }

});