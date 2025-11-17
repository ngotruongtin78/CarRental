document.addEventListener("DOMContentLoaded", function() {

    const vehicleTableBody = document.getElementById('vehicleTableBody');

    // === Modal Thêm ===
    const addVehicleBtn = document.getElementById('addVehicleBtn');
    const addModal = document.getElementById('addVehicleModal');
    const addCloseButton = addModal.querySelector('.close-button');
    const addVehicleForm = document.getElementById('addVehicleForm');

    // === Modal Sửa ===
    const editModal = document.getElementById('editVehicleModal');
    const editCloseButton = editModal.querySelector('.close-button');
    const editVehicleForm = document.getElementById('editVehicleForm');


    // --- 1. Load danh sách xe khi tải trang ---
    loadVehicles();

    async function loadVehicles() {
        try {
            const response = await fetch('/api/vehicles/admin/all');

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            let vehicles = await response.json(); // Lấy danh sách xe

            // ===================================
            // ==== SẮP XẾP DANH SÁCH XE (MỚI) ====
            // ===================================
            vehicles.sort((a, b) => {
                // Tách số ra khỏi ID (vd: "st1" -> 1, "st10" -> 10)
                const numA = parseInt(a.stationId.replace('st', ''));
                const numB = parseInt(b.stationId.replace('st', ''));

                if (numA !== numB) {
                    // Sắp xếp theo số của Station ID
                    return numA - numB;
                } else {
                    // Nếu Station ID giống nhau, sắp xếp theo biển số xe
                    return a.plate.localeCompare(b.plate);
                }
            });

            vehicleTableBody.innerHTML = '';

            // Thêm dữ liệu đã sắp xếp vào bảng
            vehicles.forEach(vehicle => {
                const tr = document.createElement('tr');

                let statusClass = 'status-available';
                let statusText = 'Sẵn sàng';
                if (!vehicle.available) {
                    statusClass = 'status-rented';
                    statusText = 'Đang thuê';
                }

                // ===================================
                // ==== SỬA "PHÚT" THÀNH "GIỜ" ====
                // ===================================
                tr.innerHTML = `
                    <td>${vehicle.plate}</td>
                    <td>${vehicle.brand} (${vehicle.type})</td>
                    <td>${vehicle.stationId}</td>
                    <td>${vehicle.battery}%</td>
                    <td>${vehicle.price.toLocaleString('vi-VN')} / giờ</td> <td><span class="status ${statusClass}">${statusText}</span></td>
                    <td>
                        <button class="btn-edit" data-id="${vehicle.id}">Sửa</button>
                        <button class="btn-delete" data-id="${vehicle.id}">Xóa</button>
                    </td>
                `;
                vehicleTableBody.appendChild(tr);
            });

        } catch (error) {
            console.error("Không thể tải danh sách xe:", error);
            vehicleTableBody.innerHTML = '<tr><td colspan="7">Lỗi khi tải dữ liệu.</td></tr>';
        }
    }

    // --- 2. Xử lý Modal THÊM ---
    addVehicleBtn.onclick = function() {
        addVehicleForm.reset();
        addModal.style.display = "block";
    }
    addCloseButton.onclick = function() {
        addModal.style.display = "none";
    }

    // --- 3. Xử lý Form Thêm xe mới ---
    addVehicleForm.addEventListener('submit', async function(event) {
        event.preventDefault();

        const formData = new FormData(addVehicleForm);
        const vehicleData = {
            plate: formData.get('plate'),
            brand: formData.get('brand'),
            type: formData.get('type'),
            battery: parseInt(formData.get('battery')),
            price: parseFloat(formData.get('price')), // Giá này là giá/giờ
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
                addVehicleForm.reset();
                loadVehicles(); // Tải lại bảng (đã có sắp xếp)
            } else {
                alert('Có lỗi xảy ra. Không thể thêm xe.');
            }
        } catch (error) {
            console.error('Lỗi khi thêm xe:', error);
            alert('Lỗi kết nối. Vui lòng thử lại.');
        }
    });

    // --- 4. Logic Sửa/Xóa (Giữ nguyên như cũ) ---

    vehicleTableBody.addEventListener('click', async function(event) {
        const target = event.target;
        const id = target.dataset.id;
        if (!id) return;

        // --- XỬ LÝ NÚT XÓA ---
        if (target.classList.contains('btn-delete')) {
            if (confirm(`Bạn có chắc muốn xóa xe có ID: ${id}?`)) {
                try {
                    const response = await fetch(`/api/vehicles/admin/delete/${id}`, {
                        method: 'DELETE'
                    });
                    if (response.ok) {
                        alert('Xóa xe thành công!');
                        loadVehicles();
                    } else {
                        alert('Xóa xe thất bại.');
                    }
                } catch (error) {
                    alert('Lỗi kết nối: ' + error.message);
                }
            }
        }

        // --- XỬ LÝ NÚT SỬA ---
        if (target.classList.contains('btn-edit')) {
            try {
                const response = await fetch(`/api/vehicles/admin/${id}`);
                if (!response.ok) throw new Error('Không tìm thấy thông tin xe');

                const vehicle = await response.json();

                editVehicleForm.querySelector('#edit-vehicleId').value = vehicle.id;
                editVehicleForm.querySelector('#edit-plate').value = vehicle.plate;
                editVehicleForm.querySelector('#edit-brand').value = vehicle.brand;
                editVehicleForm.querySelector('#edit-type').value = vehicle.type;
                editVehicleForm.querySelector('#edit-battery').value = vehicle.battery;
                editVehicleForm.querySelector('#edit-price').value = vehicle.price;
                editVehicleForm.querySelector('#edit-stationId').value = vehicle.stationId;
                editVehicleForm.querySelector('#edit-available').value = vehicle.available.toString();

                editModal.style.display = "block";

            } catch (error) {
                alert(error.message);
            }
        }
    });

    // --- 5. Xử lý Form Sửa xe (Giữ nguyên) ---
    editVehicleForm.addEventListener('submit', async function(event) {
        event.preventDefault();

        const formData = new FormData(editVehicleForm);
        const vehicleId = formData.get('id');

        const vehicleData = {
            id: vehicleId,
            plate: formData.get('plate'),
            brand: formData.get('brand'),
            type: formData.get('type'),
            battery: parseInt(formData.get('battery')),
            price: parseFloat(formData.get('price')),
            stationId: formData.get('stationId'),
            available: formData.get('available') === 'true'
        };

        try {
            const response = await fetch(`/api/vehicles/admin/update/${vehicleId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(vehicleData)
            });

            if (response.ok) {
                alert('Cập nhật (điều phối) xe thành công!');
                editModal.style.display = 'none';
                loadVehicles(); // Tải lại bảng (đã có sắp xếp)
            } else {
                alert('Có lỗi xảy ra. Không thể cập nhật xe.');
            }
        } catch (error) {
            console.error('Lỗi khi cập nhật xe:', error);
            alert('Lỗi kết nối. Vui lòng thử lại.');
        }
    });

    // --- 6. Đóng modal (Giữ nguyên) ---
    window.onclick = function(event) {
        if (event.target == addModal) {
            addModal.style.display = "none";
        }
        if (event.target == editModal) {
            editModal.style.display = "none";
        }
    }
    addCloseButton.onclick = () => addModal.style.display = "none";
    editCloseButton.onclick = () => editModal.style.display = "none";

});