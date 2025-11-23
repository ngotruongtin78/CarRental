document.addEventListener("DOMContentLoaded", function() {

    const stationTableBody = document.getElementById('stationTableBody');
    const searchInput = document.getElementById('searchInput');

    const addStationBtn = document.getElementById('addStationBtn');
    const addModal = document.getElementById('addStationModal');
    const addCloseButton = addModal.querySelector('.close-button');
    const addStationForm = document.getElementById('addStationForm');

    const editModal = document.getElementById('editStationModal');
    const editCloseButton = editModal.querySelector('.close-button');
    const editStationForm = document.getElementById('editStationForm');

    let allStationsData = [];


    window.toggleProfileMenu = function(event) {
        event.stopPropagation();
        const dropdown = document.getElementById('profileDropdown');
        if (dropdown) dropdown.classList.toggle('show');
    };

    loadStations();

    async function loadStations() {
        try {
            const response = await fetch('/api/stations/admin/all');
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);

            allStationsData = await response.json();

            renderTable(allStationsData);

        } catch (error) {
            console.error("Không thể tải danh sách trạm:", error);
            stationTableBody.innerHTML = '<tr><td colspan="4" style="text-align:center; color:red;">Lỗi khi tải dữ liệu.</td></tr>';
        }
    }

    function renderTable(data) {
        stationTableBody.innerHTML = '';

        if (!data || data.length === 0) {
            stationTableBody.innerHTML = '<tr><td colspan="4" style="text-align:center;">Không tìm thấy trạm nào phù hợp.</td></tr>';
            return;
        }

        data.forEach(station => {
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
    }

    function filterStations() {
        if (!allStationsData) return;

        const searchText = searchInput.value.toLowerCase().trim();

        const filtered = allStationsData.filter(station => {
            const name = (station.name || "").toLowerCase();
            const address = (station.address || "").toLowerCase();
            const id = (station.id || "").toLowerCase();

            return name.includes(searchText) || address.includes(searchText) || id.includes(searchText);
        });

        renderTable(filtered);
    }

    if (searchInput) {
        searchInput.addEventListener('input', filterStations);
    }

    if(addStationBtn) {
        addStationBtn.onclick = function() {
            addStationForm.reset();
            addModal.style.display = "block";
        }
    }
    if(addCloseButton) {
        addCloseButton.onclick = function() {
            addModal.style.display = "none";
        }
    }

    if(addStationForm) {
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
                    alert('Thêm trạm thành công!');
                    addModal.style.display = 'none';
                    addStationForm.reset();
                    loadStations();
                } else {
                    alert('Có lỗi xảy ra. Không thể thêm trạm.');
                }
            } catch (error) {
                alert('Lỗi kết nối. Vui lòng thử lại.');
            }
        });
    }

    stationTableBody.addEventListener('click', async function(event) {
        const target = event.target;
        const id = target.dataset.id;
        if (!id) return;

        if (target.classList.contains('btn-delete')) {
            if (confirm(`Bạn có chắc muốn xóa trạm "${id}"?`)) {
                try {
                    const response = await fetch(`/api/stations/admin/delete/${id}`, { method: 'DELETE' });
                    const text = await response.text();
                    alert(text);
                    if (response.ok) loadStations();
                } catch (error) {
                    alert('Lỗi kết nối.');
                }
            }
        }

        if (target.classList.contains('btn-edit')) {
            try {
                const response = await fetch(`/api/stations/admin/${id}`);
                if (!response.ok) throw new Error('Không tìm thấy dữ liệu');
                const station = await response.json();

                editStationForm.querySelector('#edit-id').value = station.id;
                editStationForm.querySelector('#edit-name').value = station.name;
                editStationForm.querySelector('#edit-lat').value = station.latitude;
                editStationForm.querySelector('#edit-lng').value = station.longitude;
                editStationForm.querySelector('#edit-address').value = station.address;

                editModal.style.display = 'block';
            } catch (error) {
                alert(error.message);
            }
        }
    });

    if(editStationForm) {
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
                    alert('Cập nhật thành công!');
                    editModal.style.display = 'none';
                    loadStations();
                } else {
                    alert('Lỗi cập nhật.');
                }
            } catch (error) {
                alert('Lỗi kết nối.');
            }
        });
    }

    if(editCloseButton) editCloseButton.onclick = () => editModal.style.display = "none";

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