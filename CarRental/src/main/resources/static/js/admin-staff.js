document.addEventListener("DOMContentLoaded", function() {

    const staffTableBody = document.getElementById('staffTableBody');
    const menuTemplate = document.getElementById('action-menu-template');

    const searchInput = document.getElementById('searchInput');
    const stationFilterSelect = document.getElementById('stationFilter');

    const addStaffBtn = document.getElementById('addStaffBtn');
    const addModal = document.getElementById('addStaffModal');
    const addCloseButton = addModal.querySelector('.close-button');
    const addStaffForm = document.getElementById('addStaffForm');
    const modalStationSelect = document.getElementById('modalStationId');

    const editModal = document.getElementById('editStaffModal');
    const editCloseButton = editModal.querySelector('.close-button');
    const editStaffForm = document.getElementById('editStaffForm');
    const editModalStationSelect = document.getElementById('edit-modalStationId');

    let currentOpenMenu = null;
    let stationsCache = [];
    let allStaffData = [];

    window.toggleProfileMenu = function(event) {
        event.stopPropagation();
        if (currentOpenMenu) { currentOpenMenu.remove(); currentOpenMenu = null; }
        const dropdown = document.getElementById('profileDropdown');
        if (dropdown) dropdown.classList.toggle('show');
    };

    init();

    async function init() {
        await loadStations();
        loadStaff();
    }

    async function loadStations() {
        try {
            const response = await fetch('/api/stations/admin/all');
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
        if (!staffTableBody) return;
        staffTableBody.innerHTML = '<tr><td colspan="6" style="text-align:center;">Đang tải dữ liệu...</td></tr>';

        try {
            const response = await fetch('/admin/staff/all');
            if (!response.ok) throw new Error('Failed to fetch staff');

            allStaffData = await response.json();

            renderTable(allStaffData);

        } catch (error) {
            console.error(error);
            staffTableBody.innerHTML = '<tr><td colspan="6" style="text-align:center; color:red;">Lỗi kết nối server.</td></tr>';
        }
    }

    function renderTable(data) {
        staffTableBody.innerHTML = '';

        if (!data || data.length === 0) {
            staffTableBody.innerHTML = '<tr><td colspan="6" style="text-align:center;">Không tìm thấy nhân viên phù hợp.</td></tr>';
            return;
        }

        data.forEach(staff => {
            const tr = document.createElement('tr');
            let statusText = '', statusClass = '';

            if (staff.status === 'WORKING') {
                statusText = 'Đang làm việc'; statusClass = 'status-available';
            } else if (staff.status === 'ON_LEAVE') {
                statusText = 'Nghỉ phép'; statusClass = 'status-disabled';
            } else {
                statusText = 'Đã nghỉ việc'; statusClass = 'status-inactive';
            }

            const stationObj = stationsCache.find(s => s.id === staff.stationId);
            const stationName = stationObj ? stationObj.name : (staff.stationId || 'Chưa phân công');

            tr.innerHTML = `
                <td>${staff.fullName}</td>
                <td>${stationName}</td>
                <td>${staff.role === 'ROLE_ADMIN' ? 'Quản trị' : 'Nhân viên'}</td>
                <td>${staff.performance || 0}</td>
                <td><span class="status ${statusClass}">${statusText}</span></td>
                <td class="actions">
                    <button class="action-button" data-staffid="${staff.id}">...</button>
                </td>
            `;
            staffTableBody.appendChild(tr);
        });
    }

    function filterStaff() {
        if (!allStaffData) return;

        const searchText = searchInput.value.toLowerCase().trim();
        const filterStationId = stationFilterSelect.value;

        const filtered = allStaffData.filter(staff => {
            const stationObj = stationsCache.find(s => s.id === staff.stationId);
            const stationName = stationObj ? stationObj.name.toLowerCase() : "";
            const name = (staff.fullName || "").toLowerCase();

            const matchesSearch = name.includes(searchText) || stationName.includes(searchText);

            let matchesStation = true;
            if (filterStationId !== 'all') {
                matchesStation = (staff.stationId === filterStationId);
            }

            return matchesSearch && matchesStation;
        });

        renderTable(filtered);
    }

    if (searchInput) searchInput.addEventListener('input', filterStaff);
    if (stationFilterSelect) stationFilterSelect.addEventListener('change', filterStaff);


    addStaffBtn.onclick = () => { addStaffForm.reset(); addModal.style.display = "block"; }
    addCloseButton.onclick = () => addModal.style.display = "none";

    addStaffForm.addEventListener('submit', function(e) {
        e.preventDefault();
        alert('(Giả lập) Đã thêm nhân viên mới! (Bạn cần code API create user)');
        addModal.style.display = "none";
    });

    editCloseButton.onclick = () => editModal.style.display = "none";

    staffTableBody.addEventListener('click', function(event) {
        if (event.target.classList.contains('action-button')) {
            event.stopPropagation();

            const profileDrop = document.getElementById('profileDropdown');
            if (profileDrop) profileDrop.classList.remove('show');
            if (currentOpenMenu) currentOpenMenu.remove();

            const staffId = event.target.dataset.staffid;
            if (menuTemplate) {
                const newMenu = menuTemplate.firstElementChild.cloneNode(true);

                const rect = event.target.getBoundingClientRect();
                const windowHeight = window.innerHeight;

                if (windowHeight - rect.bottom < 180) {
                    newMenu.classList.add('drop-up');
                }
                newMenu.style.display = 'block';

                newMenu.querySelector('.edit-staff').onclick = () => {
                    const staff = allStaffData.find(s => s.id === staffId);
                    if (staff) {
                        document.getElementById('edit-staffId').value = staff.id;
                        document.getElementById('edit-fullName').value = staff.fullName;
                        document.getElementById('edit-username').value = staff.username;
                        document.getElementById('edit-modalStationId').value = staff.stationId;
                        document.getElementById('edit-role').value = staff.role;
                        editModal.style.display = 'block';
                    }
                };

                event.target.parentElement.appendChild(newMenu);
                currentOpenMenu = newMenu;
            }
        }
    });
    window.addEventListener('click', function(event) {
        if (currentOpenMenu) { currentOpenMenu.remove(); currentOpenMenu = null; }
        if (event.target == addModal) addModal.style.display = "none";
        if (event.target == editModal) editModal.style.display = "none";

        if (!event.target.closest('.admin-profile')) {
            const dropdown = document.getElementById('profileDropdown');
            if (dropdown && dropdown.classList.contains('show')) {
                dropdown.classList.remove('show');
            }
        }
    });
});