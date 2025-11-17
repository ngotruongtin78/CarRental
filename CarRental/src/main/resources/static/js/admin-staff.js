document.addEventListener("DOMContentLoaded", function() {


    const staffTableBody = document.getElementById('staffTableBody');
    const menuTemplate = document.getElementById('action-menu-template');
    let currentOpenMenu = null;
    let stationsCache = [];


    const fakeData = [
        { "id": "1", "fullName": "Nguyễn Văn A", "username": "nva@station.com", "stationId": "st1", "stationName": "Đang làm việc", "role": "ROLE_ADMIN", "performance": 120, "status": "WORKING" },
        { "id": "2", "fullName": "Lê Thị B", "username": "ltb@station.com", "stationId": "st1", "stationName": "Q.1 Station", "role": "ROLE_ADMIN", "performance": 0, "status": "WORKING" },
        { "id": "3", "fullName": "Itb Đức Station", "username": "itb@station.com", "stationId": "st2", "stationName": "Thủ Đức Station", "role": "ROLE_STAFF", "performance": 85, "status": "WORKING" },
        { "id": "4", "fullName": "Phạm Văn C", "username": "pvc@station.com", "stationId": "st3", "stationName": "Bình Thạnh St...", "role": "ROLE_STAFF", "performance": 70, "status": "ON_LEAVE" },
        { "id": "5", "fullName": "Trần Đình D", "username": "tdd@station.com", "stationId": "st3", "stationName": "Bình Thạnh St...", "role": "ROLE_STAFF", "performance": 0, "status": "RESIGNED" }
    ];


    const addStaffBtn = document.getElementById('addStaffBtn');
    const addModal = document.getElementById('addStaffModal');
    const addCloseButton = addModal.querySelector('.close-button');
    const addStaffForm = document.getElementById('addStaffForm');
    const modalStationSelect = document.getElementById('modalStationId');
    const stationFilterSelect = document.getElementById('stationFilter');


    const editModal = document.getElementById('editStaffModal');
    const editCloseButton = editModal.querySelector('.close-button');
    const editStaffForm = document.getElementById('editStaffForm');
    const editModalStationSelect = document.getElementById('edit-modalStationId');


    loadStations();
    loadStaff();


    async function loadStations() {
        try {
            const response = await fetch('/api/stations');
            if (!response.ok) throw new Error('Failed to fetch stations');
            stationsCache = await response.json();


            stationFilterSelect.innerHTML = '<option value="all">Tất cả điểm thuê</option>';
            modalStationSelect.innerHTML = '<option value="">-- Chọn điểm thuê --</option>';
            editModalStationSelect.innerHTML = '<option value="">-- Chọn điểm thuê --</option>';


            stationsCache.forEach(station => {
                const option = document.createElement('option');
                option.value = station.id;
                option.textContent = station.name;

                stationFilterSelect.appendChild(option.cloneNode(true));
                modalStationSelect.appendChild(option.cloneNode(true));
                editModalStationSelect.appendChild(option.cloneNode(true));
            });

        } catch (error) {
            console.error("Không thể tải danh sách trạm:", error);
        }
    }


    async function loadStaff() {
        try {
            const staffList = fakeData;
            staffTableBody.innerHTML = '';

            staffList.forEach(staff => {
                const tr = document.createElement('tr');

                let statusText = '';
                let statusClass = '';

                switch(staff.status) {
                    case 'WORKING': statusText = 'Đang làm việc'; statusClass = 'status-available'; break;
                    case 'ON_LEAVE': statusText = 'Nghỉ phép'; statusClass = 'status-disabled'; break;
                    case 'RESIGNED': statusText = 'Đã nghỉ việc'; statusClass = 'status-inactive'; break;
                }


                const station = stationsCache.find(s => s.id === staff.stationId);
                const stationName = station ? station.name : (staff.stationId || 'Không rõ');

                tr.innerHTML = `
                    <td>${staff.fullName}</td>
                    <td>${staff.username}</td>
                    <td>${stationName}</td>
                    <td>${staff.role === 'ROLE_ADMIN' ? 'Quản trị' : 'Nhân viên'}</td>
                    <td>${staff.performance}</td>
                    <td><span class="status ${statusClass}">${statusText}</span></td>
                    <td class="actions">
                        <button class="action-button" data-staffid="${staff.id}">...</button>
                    </td>
                `;
                staffTableBody.appendChild(tr);
            });

        } catch (error) {
            console.error("Không thể tải danh sách nhân viên:", error);
            staffTableBody.innerHTML = '<tr><td colspan="7">Lỗi khi tải dữ liệu.</td></tr>';
        }
    }

    addStaffBtn.onclick = function() {
        addStaffForm.reset();
        addModal.style.display = "block";
    }
    addCloseButton.onclick = function() {
        addModal.style.display = "none";
    }
    addStaffForm.addEventListener('submit', async function(event) {
        event.preventDefault();
        const formData = new FormData(addStaffForm);
        const staffData = {
            fullName: formData.get('fullName'),
            username: formData.get('username'),
            password: formData.get('password'),
            stationId: formData.get('stationId'),
            role: formData.get('role')
        };
        try {
            alert(`(Giả lập) Đã gọi API để tạo nhân viên: ${staffData.fullName}`);
            modal.style.display = 'none';

            fakeData.push({ id: (Math.random() * 1000).toString(), ...staffData, status: 'WORKING', performance: 0 });
            loadStaff();
        } catch (error) {
            console.error('Lỗi khi thêm nhân viên:', error);
            alert('Lỗi kết nối. Vui lòng thử lại.');
        }
    });

    editCloseButton.onclick = function() {
        editModal.style.display = "none";
    }
    editStaffForm.addEventListener('submit', async function(event) {
        event.preventDefault();
        const formData = new FormData(editStaffForm);
        const staffId = formData.get('id');
        const staffData = {
            fullName: formData.get('fullName'),
            stationId: formData.get('stationId'),
            role: formData.get('role')
        };

        try {
            alert(`(Giả lập) Đã gọi API để CẬP NHẬT nhân viên ${staffId}. \nĐiều phối đến trạm: ${staffData.stationId}`);


            const index = fakeData.findIndex(s => s.id === staffId);
            if (index !== -1) {
                fakeData[index] = { ...fakeData[index], ...staffData };
            }

            editModal.style.display = 'none';
            loadStaff();
        } catch (error) {
            console.error('Lỗi khi cập nhật nhân viên:', error);
            alert('Lỗi kết nối. Vui lòng thử lại.');
        }
    });


    staffTableBody.addEventListener('click', function(event) {
        if (event.target.classList.contains('action-button')) {
            event.stopPropagation();
            if (currentOpenMenu) currentOpenMenu.remove();

            const staffId = event.target.dataset.staffid;
            const newMenu = menuTemplate.firstElementChild.cloneNode(true);
            newMenu.style.display = 'block';
            newMenu.querySelector('.edit-staff').onclick = () => {
                // Tìm nhân viên trong fakeData
                const staff = fakeData.find(s => s.id === staffId);
                if (!staff) return alert('Không tìm thấy nhân viên!');

                // Đổ dữ liệu vào form Sửa
                document.getElementById('edit-staffId').value = staff.id;
                document.getElementById('edit-fullName').value = staff.fullName;
                document.getElementById('edit-username').value = staff.username;
                document.getElementById('edit-modalStationId').value = staff.stationId;
                document.getElementById('edit-role').value = staff.role;
                document.getElementById('edit-password').value = "";


                editModal.style.display = 'block';
            };
             newMenu.querySelector('.toggle-status').onclick = () => {
                alert(`(Chức năng Thay đổi trạng thái) cho nhân viên ID: ${staffId}.`);
            };


            event.target.parentElement.appendChild(newMenu);
            currentOpenMenu = newMenu;
        }
    });
    window.addEventListener('click', function(event) {
        if (currentOpenMenu) {
            currentOpenMenu.remove();
            currentOpenMenu = null;
        }
        if (event.target == addModal) {
            addModal.style.display = "none";
        }
        if (event.target == editModal) {
            editModal.style.display = "none";
        }
    });
});