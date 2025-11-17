document.addEventListener("DOMContentLoaded", function() {

    const customerTableBody = document.getElementById('customerTableBody');
    const menuTemplate = document.getElementById('action-menu-template');
    loadCustomers();

    async function loadCustomers() {
        try {
            const fakeData = [
                { "id": "1", "fullName": "Nguyễn Văn A", "email": "nvane@email.com", "enabled": true, "verified": true, "totalTrips": 25, "totalSpent": 5500000 },
                { "id": "2", "fullName": "Lê Thị B", "email": "0912245xxx", "enabled": true, "verified": true, "totalTrips": 18, "totalSpent": 4500000 },
                { "id": "3", "fullName": "Phạm Văn C", "email": "phamvc@email.com", "enabled": true, "verified": false, "totalTrips": 0, "totalSpent": 0 },
                { "id": "4", "fullName": "Trần Đình D", "email": "dd.tran@email.com", "enabled": false, "verified": true, "totalTrips": 5, "totalSpent": 0 },
                { "id": "5", "fullName": "Mai Thị E", "email": "nvate@email.com", "enabled": true, "verified": true, "totalTrips": 30, "totalSpent": 6800000 }
            ];

            const customers = fakeData;

            customerTableBody.innerHTML = '';

            customers.forEach(user => {
                const tr = document.createElement('tr');

                // Quyết định trạng thái
                let statusText = '';
                let statusClass = '';
                if (!user.enabled) {
                    statusText = 'Đã vô hiệu';
                    statusClass = 'status-disabled';
                } else if (!user.verified) {
                    statusText = 'Chờ xác thực';
                    statusClass = 'status-pending';
                } else {
                    statusText = 'Đã kích hoạt';
                    statusClass = 'status-available';
                }

                tr.innerHTML = `
                    <td>${user.fullName}</td>
                    <td>${user.email}</td>
                    <td><span class="status ${statusClass}">${statusText}</span></td>
                    <td>${user.totalTrips}</td>
                    <td>${user.totalSpent.toLocaleString('vi-VN')} đ</td>
                    <td class="actions">
                        <button class="action-button" data-userid="${user.id}">...</button>
                    </td>
                `;
                customerTableBody.appendChild(tr);
            });

        } catch (error) {
            console.error("Không thể tải danh sách khách hàng:", error);
            customerTableBody.innerHTML = '<tr><td colspan="6">Lỗi khi tải dữ liệu.</td></tr>';
        }
    }


    let currentOpenMenu = null;

    customerTableBody.addEventListener('click', function(event) {
        if (event.target.classList.contains('action-button')) {
            event.stopPropagation();

            if (currentOpenMenu) {
                currentOpenMenu.remove();
                currentOpenMenu = null;
            }

            const userId = event.target.dataset.userid;
            const newMenu = menuTemplate.firstElementChild.cloneNode(true);
            newMenu.style.display = 'block';
            newMenu.querySelector('.view-profile').href = `/admin/customers/view/${userId}`;
            const toggleStatusLink = newMenu.querySelector('.toggle-status');
            toggleStatusLink.dataset.userId = userId;
            toggleStatusLink.addEventListener('click', (e) => toggleUserStatus(e, userId));
            event.target.parentElement.appendChild(newMenu);
            currentOpenMenu = newMenu;
        }
    });

    window.addEventListener('click', function() {
        if (currentOpenMenu) {
            currentOpenMenu.remove();
            currentOpenMenu = null;
        }
    });
    async function toggleUserStatus(event, userId) {
        event.preventDefault();
        event.stopPropagation();
        const shouldEnable = confirm("Bạn muốn thay đổi trạng thái kích hoạt của người dùng này?\n(Đây là ví dụ, cần logic rõ ràng hơn: 'Bạn muốn Kích hoạt?' hay 'Bạn muốn Vô hiệu hóa?')");

        if (!shouldEnable) return;

        try {
            alert(`(Giả lập) Đã gọi API để thay đổi trạng thái user ${userId}`);
            if (currentOpenMenu) currentOpenMenu.remove();
            loadCustomers();

        } catch (error) {
            console.error('Lỗi khi cập nhật trạng thái:', error);
        }
    }
});