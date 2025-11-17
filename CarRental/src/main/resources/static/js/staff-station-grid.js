document.addEventListener("DOMContentLoaded", function() {

    const vehicleGrid = document.getElementById('vehicle-grid');

    loadVehiclesForStation();

    async function loadVehiclesForStation() {
        try {
            // =================================================================
            // BACKEND CALL: GET /api/staff/my-station/vehicles (Giả định)
            // Mục đích: Lấy danh sách xe TẠI TRẠM HIỆN TẠI của nhân viên.
            // API này có thể DÙNG CHUNG với giao diện Dark Mode (staff-station-vehicles.js)
            //
            // Backend trả về:
            // {
            //   "stationName": "Q.1",
            //   "vehicles": [
            //     { "id": "v1", "plate": "51A-123.45", "model": "VinFast VFe34", "battery": 90,
            //       "status": "AVAILABLE", "statusText": "Sẵn sàng" },
            //     { "id": "v2", "plate": "51C-678.90", "model": "VinFast Klara", "battery": 45,
            //       "status": "RENTED", "statusText": "Đang thuê" },
            //     { "id": "v3", "plate": "51D-111.22", "model": "VinFast Theon", "battery": 30,
            //       "status": "MAINTENANCE", "statusText": "Bảo trì" }
            //   ]
            // }
            // =================================================================

            // --- GIẢ LẬP DỮ LIỆU ---
            const fakeData = {
                stationName: "Q.1",
                vehicles: [
                    { "id": "v1", "plate": "51A-123.45", "model": "VinFast VFe34", "battery": 90, "status": "AVAILABLE", "statusText": "Sẵn sàng" },
                    { "id": "v2", "plate": "51A-123.45", "model": "VinFast VFe34", "battery": 90, "status": "RENTED", "statusText": "Đang thuê" },
                    { "id": "v3", "plate": "51A-123.45", "model": "VinFast VFe34", "battery": 90, "status": "AVAILABLE", "statusText": "Sẵn sàng" },
                    { "id": "v4", "plate": "51A-123.45", "model": "VinFast VFe34", "battery": 90, "status": "AVAILABLE", "statusText": "Sẵn sàng" },
                    { "id": "v5", "plate": "51A-123.45", "model": "VinFast VFe34", "battery": 90, "status": "AVAILABLE", "statusText": "Sẵn sàng" },
                    { "id": "v6", "plate": "51A-123.45", "model": "VinFast VFe34", "battery": 30, "status": "MAINTENANCE", "statusText": "Bảo trì" }
                ]
            };
            // --- KẾT THÚC GIẢ LẬP ---
            const data = fakeData; // Dùng data giả

            vehicleGrid.innerHTML = '';
            data.vehicles.forEach(vehicle => {
                const card = document.createElement('div');
                card.className = 'vehicle-card';
                const batteryClass = vehicle.battery < 40 ? 'low' : '';
                let statusBadgeClass = 'badge-green'; // AVAILABLE
                if (vehicle.status === 'RENTED') statusBadgeClass = 'badge-orange';
                else if (vehicle.status === 'MAINTENANCE') statusBadgeClass = 'badge-red';
                else if (vehicle.status === 'CHARGING') statusBadgeClass = 'badge-blue';

                card.innerHTML = `
                    <div class="card-header">
                        <div class="plate">${vehicle.plate}</div>
                        <div class="model">${vehicle.model}</div>
                    </div>
                    <div class="card-body">
                        <div class="info-group">
                            <label>Mức pin</label>
                            <div class="value battery-value ${batteryClass}">${vehicle.battery}%</div>
                        </div>
                        <div class="info-group">
                            <label>Trạng thái</label>
                            <span class="status-badge ${statusBadgeClass}">${vehicle.statusText}</span>
                        </div>
                    </div>
                    <div class="card-footer">
                        <button class="btn-card" data-id="${vehicle.id}" data-action="update">Cập nhật</button>
                        <button class="btn-card primary" data-id="${vehicle.id}" data-action="report">Báo cáo sự cố</button>
                    </div>
                `;
                vehicleGrid.appendChild(card);
            });

        } catch (error)
        {
            console.error("Lỗi tải danh sách xe:", error);
            vehicleGrid.innerHTML = '<p style="color: #e74c3c;">Lỗi khi tải dữ liệu xe.</p>';
        }
    }
    vehicleGrid.addEventListener('click', (event) => {
        const target = event.target;
        if (target.tagName !== 'BUTTON' || !target.dataset.id) return;

        const vehicleId = target.dataset.id;
        const action = target.dataset.action;

        if (action === 'update') {
            // =================================================================
            // BACKEND CALL: (Mở Modal)
            // Mục đích: Mở 1 modal để nhân viên cập nhật pin/trạng thái.
            // =================================================================
            alert(`(Giả lập) Mở modal Cập nhật cho xe ID: ${vehicleId}`);
        }
        else if (action === 'report') {
            // =================================================================
            // BACKEND CALL: POST /api/staff/report-issue (Giả định)
            // Mục đích: Báo cáo sự cố cho xe.
            // =================================================================
            const issue = prompt(`(Giả lập) Nhập sự cố cho xe ID: ${vehicleId}`);
            if (issue) {
                alert(`(Giả lập) Gửi báo cáo: "${issue}"`);
            }
        }
    });

});