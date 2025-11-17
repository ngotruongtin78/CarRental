document.addEventListener("DOMContentLoaded", function() {

    const stationTableBody = document.getElementById('stationTableBody');

    // === Modal Thêm ===
    const addStationBtn = document.getElementById('addStationBtn');
    const addModal = document.getElementById('addStationModal');
    const addCloseButton = addModal.querySelector('.close-button');
    const addStationForm = document.getElementById('addStationForm');

    // === Modal Sửa (Lấy từ HTML) ===
    const editModal = document.getElementById('editStationModal');
    const editCloseButton = editModal.querySelector('.close-button');
    const editStationForm = document.getElementById('editStationForm');

    // --- 1. Load danh sách trạm ---
    loadStations();

    async function loadStations() {
        try {
            const response = await fetch('/api/stations/admin/all');
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);

            const stations = await response.json();
            stationTableBody.innerHTML = '';

            stations.forEach(station => {
                const tr = document.createElement('tr');
                const addressText = station.address ? station.address : "Chưa cập nhật";

                tr.innerHTML = `
                    <td>${station.id}</td>
                    <td>${station.name}</td>
                    <td>
                        <span style="display: block;">${addressText}</span>
                        <small style="color: #555;">(Lat: ${station.latitude}, Lng: ${station.longitude})</small>
                    </td>
                    <td>
                        <button class="btn-edit" data-id="${station.id}">Sửa</button>
                        <button class="btn-delete" data-id="${station.id}">Xóa</button>
                    </td>
                `;
                stationTableBody.appendChild(tr);
            });

        } catch (error) {
            console.error("Không thể tải danh sách trạm:", error);
            stationTableBody.innerHTML = '<tr><td colspan="4">Lỗi khi tải dữ liệu.</td></tr>';
        }
    }

    // --- 2. Xử lý Modal THÊM ---
    addStationBtn.onclick = function() {
        addStationForm.reset();
        addModal.style.display = "block";
    }
    addCloseButton.onclick = function() {
        addModal.style.display = "none";
    }

    // --- 3. Xử lý Form Thêm trạm mới ---
    addStationForm.addEventListener('submit', async function(event) {
        event.preventDefault();
        const formData = new FormData(addStationForm);
        const stationData = {
            name: formData.get('name'),
            latitude: parseFloat(formData.get('lat')),
            longitude: parseFloat(formData.get('lng')),
            address: formData.get('address')
        };

        try {
            const response = await fetch('/api/stations/admin/add', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(stationData)
            });

            if (response.ok) {
                alert('Thêm trạm thành công! ID đã được tạo tự động.');
                addModal.style.display = 'none';
                addStationForm.reset();
                loadStations();
            } else {
                alert('Có lỗi xảy ra. Không thể thêm trạm.');
            }
        } catch (error) {
            console.error('Lỗi khi thêm trạm:', error);
            alert('Lỗi kết nối. Vui lòng thử lại.');
        }
    });

    // ===================================
    // ==== LOGIC MỚI CHO SỬA VÀ XÓA ====
    // ===================================

    // --- 4. Bắt sự kiện click Sửa hoặc Xóa trên Bảng ---
    stationTableBody.addEventListener('click', async function(event) {
        const target = event.target;
        const id = target.dataset.id;

        if (!id) return; // Không làm gì nếu click ra ngoài

        // --- XỬ LÝ NÚT XÓA ---
        if (target.classList.contains('btn-delete')) {
            if (confirm(`Bạn có chắc muốn xóa trạm "${id}"? \nLƯU Ý: Chỉ xóa được khi trạm không còn xe.`)) {
                try {
                    const response = await fetch(`/api/stations/admin/delete/${id}`, {
                        method: 'DELETE'
                    });

                    const responseText = await response.text();

                    if (response.ok) {
                        alert(responseText); // "Xóa trạm st1 thành công!"
                        loadStations(); // Tải lại bảng
                    } else {
                        // "Không thể xóa trạm vì vẫn còn xe."
                        alert(responseText);
                    }
                } catch (error) {
                    alert('Lỗi kết nối: ' + error.message);
                }
            }
        }

        // --- XỬ LÝ NÚT SỬA ---
        if (target.classList.contains('btn-edit')) {
            try {
                // Gọi API mới để lấy thông tin chi tiết
                const response = await fetch(`/api/stations/admin/${id}`);
                if (!response.ok) {
                    // Nếu server trả về 404 (do chưa restart)
                    throw new Error('Không tìm thấy dữ liệu trạm.');
                }

                const station = await response.json();

                // Đổ dữ liệu vào form Sửa
                editStationForm.querySelector('#edit-id').value = station.id;
                editStationForm.querySelector('#edit-name').value = station.name;
                editStationForm.querySelector('#edit-lat').value = station.latitude;
                editStationForm.querySelector('#edit-lng').value = station.longitude;
                editStationForm.querySelector('#edit-address').value = station.address;

                // Mở modal Sửa
                editModal.style.display = 'block';

            } catch (error) {
                alert(error.message); // Hiển thị "Không tìm thấy dữ liệu trạm."
            }
        }
    });

    // --- 5. Xử lý Form Sửa trạm ---
    editStationForm.addEventListener('submit', async function(event) {
        event.preventDefault();
        const formData = new FormData(editStationForm);
        const id = formData.get('id');

        const stationData = {
            id: id,
            name: formData.get('name'),
            latitude: parseFloat(formData.get('lat')),
            longitude: parseFloat(formData.get('lng')),
            address: formData.get('address')
        };

        try {
            const response = await fetch(`/api/stations/admin/update/${id}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(stationData)
            });

            if (response.ok) {
                alert('Cập nhật trạm thành công!');
                editModal.style.display = 'none';
                loadStations(); // Tải lại bảng
            } else {
                alert('Có lỗi xảy ra. Không thể cập nhật trạm.');
            }
        } catch (error) {
            alert('Lỗi kết nối: ' + error.message);
        }
    });

    // --- 6. Đóng 2 modal ---
    editCloseButton.onclick = function() {
        editModal.style.display = "none";
    }
    window.onclick = function(event) {
        if (event.target == addModal) {
            addModal.style.display = "none";
        }
        if (event.target == editModal) {
            editModal.style.display = "none";
        }
    }
});