document.addEventListener("DOMContentLoaded", function() {

    const customerTableBody = document.getElementById('customerTableBody');
    const searchInput = document.getElementById('searchInput');
    const statusFilter = document.getElementById('statusFilter');
    const menuTemplate = document.getElementById('action-menu-template');

    let currentOpenMenu = null;
    let allCustomersData = [];

    window.toggleProfileMenu = function(event) {
        event.stopPropagation();
        if (currentOpenMenu) { currentOpenMenu.remove(); currentOpenMenu = null; }
        const dropdown = document.getElementById('profileDropdown');
        if (dropdown) dropdown.classList.toggle('show');
    };

    loadCustomers();

    async function loadCustomers() {
        if (!customerTableBody) return;
        customerTableBody.innerHTML = '<tr><td colspan="5" style="text-align:center;">Đang tải dữ liệu...</td></tr>';

        try {
            const response = await fetch('/admin/customers/all');
            if (!response.ok) throw new Error("Lỗi tải dữ liệu");

            allCustomersData = await response.json();
            filterData();

        } catch (error) {
            console.error(error);
            customerTableBody.innerHTML = '<tr><td colspan="5" style="text-align:center; color:red;">Lỗi kết nối server.</td></tr>';
        }
    }

    function renderTable(data) {
        customerTableBody.innerHTML = '';

        if (!data || data.length === 0) {
            customerTableBody.innerHTML = '<tr><td colspan="5" style="text-align:center;">Không tìm thấy dữ liệu.</td></tr>';
            return;
        }

        data.forEach(user => {
            const tr = document.createElement('tr');
            let statusText = '', statusClass = '';

            const isEnabled = user.enabled === true;
            const isVerified = user.verified === true;
            const isRisk = user.risk === true;

            if (!isEnabled) {
                statusText = 'Đã vô hiệu'; statusClass = 'status-disabled';
            } else if (!isVerified) {
                statusText = 'Chờ xác thực'; statusClass = 'status-pending';
            } else {
                statusText = 'Đã kích hoạt'; statusClass = 'status-available';
            }

            const fullName = user.fullName || user.username || "Không tên";

            let nameDisplay = fullName;
            if (isRisk) {
                nameDisplay += ` <span style="color:#e74c3c; font-weight:bold; font-size:11px; border:1px solid #e74c3c; padding:1px 4px; border-radius:4px; margin-left:5px;">⚠️ RỦI RO</span>`;
            }

            const totalSpent = user.totalSpent ? user.totalSpent.toLocaleString('vi-VN') : '0';
            const totalTrips = user.totalTrips || 0;

            tr.innerHTML = `
                <td>${nameDisplay}</td>
                <td><span class="status ${statusClass}">${statusText}</span></td>
                <td>${totalTrips}</td>
                <td>${totalSpent} đ</td>
                <td class="actions">
                    <button class="action-button"
                            data-userid="${user.id}"
                            data-enabled="${isEnabled}"
                            data-risk="${isRisk}">...</button>
                </td>
            `;
            customerTableBody.appendChild(tr);
        });
    }

    function filterData() {
        if (!allCustomersData) return;

        const searchText = searchInput ? searchInput.value.toLowerCase().trim() : "";
        const statusValue = statusFilter ? statusFilter.value : "all";

        const filtered = allCustomersData.filter(user => {
            const name = (user.fullName || "").toLowerCase();
            const username = (user.username || "").toLowerCase();
            const matchesSearch = name.includes(searchText) || username.includes(searchText);

            let matchesStatus = true;
            if (statusValue !== 'all') {
                const isEnabled = user.enabled === true;
                const isVerified = user.verified === true;

                if (statusValue === 'disabled') matchesStatus = !isEnabled;
                else if (statusValue === 'pending') matchesStatus = (isEnabled && !isVerified);
                else if (statusValue === 'active') matchesStatus = (isEnabled && isVerified);
            }

            return matchesSearch && matchesStatus;
        });

        renderTable(filtered);
    }

    if (statusFilter) statusFilter.addEventListener('change', filterData);
    if (searchInput) searchInput.addEventListener('input', filterData);

    customerTableBody.addEventListener('click', function(event) {
        if (event.target.classList.contains('action-button')) {
            event.stopPropagation();

            if (currentOpenMenu) { currentOpenMenu.remove(); currentOpenMenu = null; }
            const dropdown = document.getElementById('profileDropdown');
            if (dropdown) dropdown.classList.remove('show');

            const userId = event.target.dataset.userid;
            const isEnabled = event.target.dataset.enabled === 'true';
            const isRisk = event.target.dataset.risk === 'true';

            if (menuTemplate) {
                const newMenu = menuTemplate.firstElementChild.cloneNode(true);

                const rect = event.target.getBoundingClientRect();
                if (window.innerHeight - rect.bottom < 160) {
                    newMenu.classList.add('drop-up');
                }

                newMenu.style.display = 'block';

                const viewLink = newMenu.querySelector('.view-profile');
                if(viewLink) viewLink.href = `/admin/customers/view/${userId}`;

                const toggleLink = newMenu.querySelector('.toggle-status');
                if(toggleLink) {
                    toggleLink.textContent = isEnabled ? "Vô hiệu hóa" : "Hủy vô hiệu hóa";
                    toggleLink.style.color = isEnabled ? '#e74c3c' : '#2ecc71';
                    toggleLink.onclick = (e) => toggleUserStatus(e, userId, isEnabled);
                }

                const riskLink = newMenu.querySelector('.mark-risk');
                if(riskLink) {
                    riskLink.textContent = isRisk ? "Gỡ đánh dấu rủi ro" : "Đánh dấu rủi ro";
                    riskLink.style.color = isRisk ? '#2ecc71' : '#e67e22';
                    riskLink.onclick = (e) => toggleUserRisk(e, userId, isRisk);
                }

                event.target.parentElement.appendChild(newMenu);
                currentOpenMenu = newMenu;
            }
        }
    });

    window.addEventListener('click', function(event) {
        if (currentOpenMenu) { currentOpenMenu.remove(); currentOpenMenu = null; }
        if (!event.target.closest('.admin-profile')) {
            const dropdown = document.getElementById('profileDropdown');
            if (dropdown) dropdown.classList.remove('show');
        }
    });

    async function toggleUserStatus(event, userId, isCurrentlyEnabled) {
        event.preventDefault();
        const actionName = isCurrentlyEnabled ? "VÔ HIỆU HÓA" : "KÍCH HOẠT LẠI";
        if (!confirm(`Bạn có chắc chắn muốn ${actionName} tài khoản này?`)) return;

        try {
            const response = await fetch(`/admin/customers/toggle-status/${userId}`, { method: 'POST' });
            if (response.ok) {
                const result = await response.text();
                alert(result === 'ACTIVATED' ? 'Đã kích hoạt thành công!' : 'Đã vô hiệu hóa tài khoản!');
                if (currentOpenMenu) currentOpenMenu.remove();
                loadCustomers();
            } else {
                alert("Lỗi: Không thể cập nhật trạng thái.");
            }
        } catch (error) {
            console.error(error);
            alert("Lỗi kết nối đến máy chủ.");
        }
    }

    async function toggleUserRisk(event, userId, isCurrentlyRisk) {
        event.preventDefault();

        let actionName = "";
        let explanation = "";

        if (isCurrentlyRisk) {
            actionName = "GỠ BỎ đánh dấu rủi ro";
            explanation = "(Sau khi gỡ, khách hàng sẽ được thuê xe bình thường và không còn bị cảnh báo)";
        } else {
            actionName = "ĐÁNH DẤU là khách hàng rủi ro";
            explanation = "(Hệ thống sẽ cảnh báo nhân viên kiểm tra kỹ khi khách này thuê xe)";
        }

        if (!confirm(`Bạn có muốn ${actionName} cho khách hàng này?\n${explanation}`)) return;

        try {
            const response = await fetch(`/admin/customers/toggle-risk/${userId}`, { method: 'POST' });
            if (response.ok) {
                const result = await response.text();
                if (result === 'RISK_MARKED') {
                    alert('Đã đánh dấu RỦI RO thành công!');
                } else {
                    alert('Đã GỠ BỎ trạng thái rủi ro thành công!');
                }

                if (currentOpenMenu) currentOpenMenu.remove();
                loadCustomers();
            } else {
                alert("Lỗi server khi cập nhật rủi ro.");
            }
        } catch (error) {
            console.error(error);
            alert("Lỗi kết nối.");
        }
    }
});