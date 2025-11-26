document.addEventListener("DOMContentLoaded", function() {

    const staffTableBody = document.getElementById('staffTableBody');
    const menuTemplate = document.getElementById('action-menu-template');
    const searchInput = document.getElementById('searchInput');
    const stationFilterSelect = document.getElementById('stationFilter');
    const addStaffBtn = document.getElementById('addStaffBtn');
    const addModal = document.getElementById('addStaffModal');
    const addCloseButton = addModal.querySelector('.close-button');
    const addStaffForm = document.getElementById('addStaffForm');
    const editModal = document.getElementById('editStaffModal');
    const editCloseButton = editModal.querySelector('.close-button');
    const editStaffForm = document.getElementById('editStaffForm');

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
            const opts = '<option value="">-- Chọn điểm thuê --</option>' +
                stationsCache.map(s => `<option value="${s.id}">${s.name}</option>`).join('');

            if(document.getElementById('modalStationId')) document.getElementById('modalStationId').innerHTML = opts;
            if(document.getElementById('edit-modalStationId')) document.getElementById('edit-modalStationId').innerHTML = opts;

            stationsCache.forEach(s => {
                const opt = document.createElement('option');
                opt.value = s.id; opt.textContent = s.name;
                stationFilterSelect.appendChild(opt);
            });
        } catch (error) { console.error("Lỗi tải trạm:", error); }
    }

    async function loadStaff() {
        if (!staffTableBody) return;
        staffTableBody.innerHTML = '<tr><td colspan="6" style="text-align:center;">Đang tải...</td></tr>';
        try {
            const response = await fetch('/admin/staff/all');
            if (!response.ok) throw new Error('Failed to fetch staff');
            allStaffData = await response.json();
            renderTable(allStaffData);
        } catch (error) {
            staffTableBody.innerHTML = '<tr><td colspan="6" style="text-align:center; color:red;">Lỗi kết nối.</td></tr>';
        }
    }

    function renderTable(data) {
        staffTableBody.innerHTML = '';
        if (!data || data.length === 0) {
            staffTableBody.innerHTML = '<tr><td colspan="6" style="text-align:center;">Không có dữ liệu.</td></tr>';
            return;
        }
        data.forEach(staff => {
            const tr = document.createElement('tr');
            let statusText = staff.status === 'WORKING' ? 'Đang làm việc' : 'Đã nghỉ việc';
            let statusClass = staff.status === 'WORKING' ? 'status-available' : 'status-inactive';

            const stationObj = stationsCache.find(s => s.id === staff.stationId);
            const stationName = stationObj ? stationObj.name : (staff.stationId || 'Chưa phân công');

            tr.innerHTML = `
                <td>${staff.fullName}</td>
                <td>${stationName}</td>
                <td>${staff.role === 'ROLE_ADMIN' ? 'Quản trị' : 'Nhân viên'}</td>
                <td>${staff.performance || 0}</td>
                <td><span class="status ${statusClass}">${statusText}</span></td>
                <td class="actions"><button class="action-button" data-staffid="${staff.id}">...</button></td>
            `;
            staffTableBody.appendChild(tr);
        });
    }

    if(editStaffForm) {
        editStaffForm.addEventListener('submit', async function(event) {
            event.preventDefault();
            const id = document.getElementById('edit-staffId').value;
            const payload = {
                fullName: document.getElementById('edit-fullName').value,
                password: document.getElementById('edit-password').value,
                stationId: document.getElementById('edit-modalStationId').value,
                role: document.getElementById('edit-role').value
            };
            try {
                const response = await fetch(`/admin/staff/update/${id}`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });
                if (response.ok) {
                    alert('Cập nhật thành công!');
                    editModal.style.display = 'none';
                    editStaffForm.reset();
                    loadStaff();
                } else { alert('Lỗi: ' + await response.text()); }
            } catch (error) { alert('Lỗi kết nối.'); }
        });
    }

    function filterStaff() {
        if (!allStaffData) return;
        const searchText = searchInput.value.toLowerCase().trim();
        const filterStationId = stationFilterSelect.value;
        const filtered = allStaffData.filter(staff => {
            const sName = (stationsCache.find(s => s.id === staff.stationId)?.name || "").toLowerCase();
            const name = (staff.fullName || "").toLowerCase();
            const matchSearch = name.includes(searchText) || sName.includes(searchText);
            const matchStation = filterStationId === 'all' || staff.stationId === filterStationId;
            return matchSearch && matchStation;
        });
        renderTable(filtered);
    }
    if (searchInput) searchInput.addEventListener('input', filterStaff);
    if (stationFilterSelect) stationFilterSelect.addEventListener('change', filterStaff);

    addStaffBtn.onclick = () => { addStaffForm.reset(); addModal.style.display = "block"; }
    addCloseButton.onclick = () => addModal.style.display = "none";
    editCloseButton.onclick = () => editModal.style.display = "none";
    addStaffForm.addEventListener('submit', e => { e.preventDefault(); alert('Cần API Create User'); addModal.style.display = "none"; });

    staffTableBody.addEventListener('click', function(event) {
        if (event.target.classList.contains('action-button')) {
            event.stopPropagation();
            if (currentOpenMenu) currentOpenMenu.remove();
            const staffId = event.target.dataset.staffid;
            if (menuTemplate) {
                const newMenu = menuTemplate.firstElementChild.cloneNode(true);
                newMenu.style.display = 'block';
                const rect = event.target.getBoundingClientRect();
                if (window.innerHeight - rect.bottom < 180) newMenu.classList.add('drop-up');

                newMenu.querySelector('.edit-staff').onclick = () => {
                    const staff = allStaffData.find(s => s.id === staffId);
                    if (staff) {
                        document.getElementById('edit-staffId').value = staff.id;
                        document.getElementById('edit-fullName').value = staff.fullName;
                        document.getElementById('edit-username').value = staff.username;
                        document.getElementById('edit-modalStationId').value = staff.stationId || "";
                        document.getElementById('edit-role').value = staff.role;
                        editModal.style.display = 'block';
                    }
                    if (currentOpenMenu) currentOpenMenu.remove();
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
            if (dropdown && dropdown.classList.contains('show')) dropdown.classList.remove('show');
        }
    });
});