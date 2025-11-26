document.addEventListener("DOMContentLoaded", function() {

    const vehicleTableBody = document.getElementById('vehicleTableBody');
    const searchInput = document.getElementById('searchInput');
    const statusFilter = document.getElementById('statusFilter'); // [MỚI]

    const addVehicleBtn = document.getElementById('addVehicleBtn');
    const addModal = document.getElementById('addVehicleModal');
    const addCloseButton = addModal.querySelector('.close-button');
    const addVehicleForm = document.getElementById('addVehicleForm');

    const editModal = document.getElementById('editVehicleModal');
    const editCloseButton = editModal.querySelector('.close-button');
    const editVehicleForm = document.getElementById('editVehicleForm');

    let allVehiclesData = [];

    window.toggleProfileMenu = function(event) {
        event.stopPropagation();
        const dropdown = document.getElementById('profileDropdown');
        if (dropdown) dropdown.classList.toggle('show');
    };

    window.addEventListener('click', function(event) {
        if (!event.target.closest('.admin-profile')) {
            const dropdown = document.getElementById('profileDropdown');
            if (dropdown && dropdown.classList.contains('show')) {
                dropdown.classList.remove('show');
            }
        }
    });

    loadVehicles();

    async function loadVehicles() {
        if (!vehicleTableBody) return;
        vehicleTableBody.innerHTML = '<tr><td colspan="7" style="text-align:center;">Đang tải dữ liệu...</td></tr>';

        try {
            const response = await fetch('/api/vehicles/admin/all');
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);

            allVehiclesData = await response.json();

            // Sắp xếp: StationID -> Biển số
            allVehiclesData.sort((a, b) => {
                const numA = parseInt(a.stationId.replace('st', '')) || 0;
                const numB = parseInt(b.stationId.replace('st', '')) || 0;
                if (numA !== numB) return numA - numB;
                return a.plate.localeCompare(b.plate);
            });

            // Gọi hàm lọc ngay khi tải xong (để áp dụng nếu có giá trị mặc định)
            filterVehicles();

        } catch (error) {
            console.error("Lỗi:", error);
            vehicleTableBody.innerHTML = '<tr><td colspan="7" style="text-align:center; color:red;">Lỗi khi tải dữ liệu.</td></tr>';
        }
    }

    function renderTable(data) {
        vehicleTableBody.innerHTML = '';

        if (!data || data.length === 0) {
            vehicleTableBody.innerHTML = '<tr><td colspan="7" style="text-align:center;">Không tìm thấy xe phù hợp.</td></tr>';
            return;
        }

        data.forEach(vehicle => {
            const tr = document.createElement('tr');
            let statusClass = 'status-available';
            let statusText = 'Sẵn sàng';

            if (vehicle.bookingStatus === 'PENDING_PAYMENT') {
                statusClass = 'status-pending';
                statusText = 'Chờ thanh toán';
            } else if (vehicle.bookingStatus === 'RENTED' || !vehicle.available) {
                statusClass = 'status-rented';
                statusText = 'Đang thuê';
            } else if (vehicle.bookingStatus === 'MAINTENANCE') {
                statusClass = 'status-maintenance';
                statusText = 'Bảo trì';
            }

            const price = vehicle.price ? vehicle.price.toLocaleString('vi-VN') : '0';

            tr.innerHTML = `
                <td>${vehicle.plate}</td>
                <td>${vehicle.brand} (${vehicle.type})</td>
                <td>${vehicle.stationId}</td>
                <td>${vehicle.battery}%</td>
                <td>${price} / ngày</td>
                <td><span class="status ${statusClass}">${statusText}</span></td>
                <td>
                    <button class="btn-edit" data-id="${vehicle.id}">Sửa</button>
                    <button class="btn-delete" data-id="${vehicle.id}">Xóa</button>
                </td>
            `;
            vehicleTableBody.appendChild(tr);
        });
    }

    // [CẬP NHẬT] Hàm lọc kết hợp: Tìm kiếm + Trạng thái
    function filterVehicles() {
        if (!allVehiclesData) return;

        const searchText = searchInput.value.toLowerCase().trim();
        const filterStatus = statusFilter ? statusFilter.value : 'all';

        const filtered = allVehiclesData.filter(vehicle => {
            // 1. Điều kiện tìm kiếm
            const plate = (vehicle.plate || "").toLowerCase();
            const brand = (vehicle.brand || "").toLowerCase();
            const type = (vehicle.type || "").toLowerCase();
            const stationId = (vehicle.stationId || "").toLowerCase();

            const matchesSearch = plate.includes(searchText) ||
                                  brand.includes(searchText) ||
                                  type.includes(searchText) ||
                                  stationId.includes(searchText);

            // 2. Điều kiện trạng thái
            let matchesStatus = true;
            // Mặc định nếu null thì coi như AVAILABLE để dễ lọc
            const status = vehicle.bookingStatus || 'AVAILABLE';

            if (filterStatus === 'available') {
                matchesStatus = (status === 'AVAILABLE');
            } else if (filterStatus === 'rented') {
                matchesStatus = (status === 'RENTED' || status === 'PENDING_PAYMENT');
            } else if (filterStatus === 'maintenance') {
                matchesStatus = (status === 'MAINTENANCE');
            }

            return matchesSearch && matchesStatus;
        });

        renderTable(filtered);
    }

    if (searchInput) searchInput.addEventListener('input', filterVehicles);
    if (statusFilter) statusFilter.addEventListener('change', filterVehicles);

    // --- XỬ LÝ THÊM MỚI ---
    addVehicleBtn.onclick = () => { addVehicleForm.reset(); addModal.style.display = "block"; }
    addCloseButton.onclick = () => addModal.style.display = "none";

    addVehicleForm.addEventListener('submit', async function(event) {
        event.preventDefault();
        const formData = new FormData(addVehicleForm);
        const vehicleData = {
            plate: formData.get('plate'),
            brand: formData.get('brand'),
            type: formData.get('type'),
            battery: parseInt(formData.get('battery')),
            price: parseFloat(formData.get('price')),
            stationId: formData.get('stationId')
        };

        try {
            const response = await fetch('/api/vehicles/admin/add', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(vehicleData)
            });
            if (response.ok) {
                alert('Thêm xe thành công!');
                addModal.style.display = 'none';
                loadVehicles();
            } else {
                alert('Lỗi khi thêm xe.');
            }
        } catch (error) {
            alert('Lỗi kết nối.');
        }
    });

    // --- XỬ LÝ SỬA / XÓA ---
    vehicleTableBody.addEventListener('click', async function(event) {
        const target = event.target;
        const id = target.dataset.id;
        if (!id) return;

        // XÓA
        if (target.classList.contains('btn-delete')) {
            if (confirm(`Bạn có chắc muốn xóa xe ID: ${id}?`)) {
                try {
                    const response = await fetch(`/api/vehicles/admin/delete/${id}`, { method: 'DELETE' });
                    alert(await response.text());
                    if (response.ok) loadVehicles();
                } catch (e) { alert('Lỗi kết nối: ' + e.message); }
            }
        }

        // SỬA
        if (target.classList.contains('btn-edit')) {
            try {
                const response = await fetch(`/api/vehicles/admin/${id}`);
                if (!response.ok) throw new Error('Không tìm thấy xe');
                const vehicle = await response.json();

                // Điền dữ liệu vào Form
                editVehicleForm.querySelector('#edit-vehicleId').value = vehicle.id;
                editVehicleForm.querySelector('#edit-plate').value = vehicle.plate;
                editVehicleForm.querySelector('#edit-brand').value = vehicle.brand;
                editVehicleForm.querySelector('#edit-type').value = vehicle.type;
                editVehicleForm.querySelector('#edit-battery').value = vehicle.battery;
                editVehicleForm.querySelector('#edit-price').value = vehicle.price;
                editVehicleForm.querySelector('#edit-stationId').value = vehicle.stationId;

                // Xử lý trạng thái (Cho phép chọn thủ công 3 loại)
                const statusSelect = editVehicleForm.querySelector('#edit-bookingStatus');
                if(statusSelect) {
                    if (['AVAILABLE', 'RENTED', 'MAINTENANCE'].includes(vehicle.bookingStatus)) {
                        statusSelect.value = vehicle.bookingStatus;
                    } else if (vehicle.bookingStatus === 'PENDING_PAYMENT') {
                        statusSelect.value = 'RENTED';
                    } else {
                        statusSelect.value = 'AVAILABLE';
                    }
                }

                editModal.style.display = "block";
            } catch (e) { alert(e.message); }
        }
    });

    // --- XỬ LÝ LƯU CẬP NHẬT ---
    editVehicleForm.addEventListener('submit', async function(event) {
        event.preventDefault();
        const formData = new FormData(editVehicleForm);
        const id = formData.get('id');

        // Lấy trạng thái từ dropdown
        const selectedStatus = formData.get('bookingStatus');
        // Tự động cập nhật available: chỉ True khi chọn Sẵn sàng
        const isAvailable = (selectedStatus === 'AVAILABLE');

        const vehicleData = {
            id: id,
            plate: formData.get('plate'),
            brand: formData.get('brand'),
            type: formData.get('type'),
            battery: parseInt(formData.get('battery')),
            price: parseFloat(formData.get('price')),
            stationId: formData.get('stationId'),

            // Gửi cả 2 trường
            bookingStatus: selectedStatus,
            available: isAvailable
        };

        try {
            const response = await fetch(`/api/vehicles/admin/update/${id}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(vehicleData)
            });
            if (response.ok) {
                alert('Cập nhật thành công!');
                editModal.style.display = 'none';
                loadVehicles();
            } else {
                alert('Lỗi cập nhật.');
            }
        } catch (e) { alert('Lỗi kết nối.'); }
    });

    editCloseButton.onclick = () => editModal.style.display = "none";

    window.onclick = function(event) {
        if (event.target == addModal) addModal.style.display = "none";
        if (event.target == editModal) editModal.style.display = "none";
        if (!event.target.closest('.admin-profile')) {
            const dropdown = document.getElementById('profileDropdown');
            if (dropdown && dropdown.classList.contains('show')) {
                dropdown.classList.remove('show');
            }
        }
    }
});