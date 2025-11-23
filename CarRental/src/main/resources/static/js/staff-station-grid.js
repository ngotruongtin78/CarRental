document.addEventListener("DOMContentLoaded", function() {

    const vehicleGrid = document.getElementById('vehicle-grid');

    loadVehiclesForStation();

    async function loadVehiclesForStation() {
        try {
            const stationResponse = await fetch('/api/staff/current-station');
            console.log("Response station status:", stationResponse.status);

            if (!stationResponse.ok) {
                const errorData = await stationResponse.text();
                throw new Error(`Không thể lấy thông tin trạm (${stationResponse.status}): ${errorData}`);
            }

            const stationData = await stationResponse.json();
            console.log("Dữ liệu trạm:", stationData);
            const stationId = stationData.id;

            if (!stationId) {
                throw new Error('Không tìm thấy station ID');
            }
            console.log("Bước 2: Lấy danh sách xe từ trạm:", stationId);
            const vehicleResponse = await fetch(`/api/vehicles/station/${stationId}/staff-station`);
            console.log("Response vehicles status:", vehicleResponse.status);

            if (!vehicleResponse.ok) {
                const errorData = await vehicleResponse.text();
                throw new Error(`Không thể lấy danh sách xe (${vehicleResponse.status}): ${errorData}`);
            }

            const data = await vehicleResponse.json();
            console.log("Dữ liệu vehicles:", data);

            if (!data.vehicles || !Array.isArray(data.vehicles)) {
                throw new Error('Format dữ liệu không đúng: vehicles phải là array');
            }

            vehicleGrid.innerHTML = '';

            const formatModel = (vehicle) => {
                return vehicle.model || `${vehicle.brand || ''} ${vehicle.type || ''}`.trim() || 'Unknown';
            };

            const formatStatus = (bookingStatus) => {
                const statusMap = {
                    'AVAILABLE': 'Sẵn sàng',
                    'PENDING_PAYMENT': 'Chờ thanh toán',
                    'RENTED': 'Đang thuê',
                    'MAINTENANCE': 'Bảo trì',
                    'CHARGING': 'Đang sạc'
                };
                return statusMap[bookingStatus] || bookingStatus;
            };

            data.vehicles.forEach(vehicle => {
                const card = document.createElement('div');
                card.className = 'vehicle-card';
                const batteryClass = vehicle.battery < 40 ? 'low' : '';
                let statusBadgeClass = 'badge-green'; // AVAILABLE
                if (vehicle.bookingStatus === 'RENTED') statusBadgeClass = 'badge-orange';
                else if (vehicle.bookingStatus === 'MAINTENANCE') statusBadgeClass = 'badge-red';
                else if (vehicle.bookingStatus === 'CHARGING') statusBadgeClass = 'badge-blue';
                else if (vehicle.bookingStatus === 'PENDING_PAYMENT') statusBadgeClass = 'badge-yellow';

                const modelName = formatModel(vehicle);
                const statusText = formatStatus(vehicle.bookingStatus);

                const getSeverityClass = (severity) => {
                    const severityMap = {
                        'MINOR': 'severity-minor',
                        'MODERATE': 'severity-moderate',
                        'CRITICAL': 'severity-critical'
                    };
                    return severityMap[severity] || '';
                };

                const formatSeverity = (severity) => {
                    const severityMap = {
                        'MINOR': 'Nhẹ',
                        'MODERATE': 'Trung bình',
                        'CRITICAL': 'Nghiêm trọng'
                    };
                    return severityMap[severity] || severity;
                };

                const issueSection = vehicle.issue ? `
                    <div class="issue-section ${getSeverityClass(vehicle.issueSeverity)}">
                        <div class="issue-label">⚠️ Sự cố</div>
                        <div class="issue-text">${vehicle.issue}</div>
                        <div class="issue-severity">Mức độ: ${formatSeverity(vehicle.issueSeverity)}</div>
                    </div>
                ` : '';

                card.innerHTML = `
                    <div class="card-header">
                        <div class="plate">${vehicle.plate || 'N/A'}</div>
                        <div class="model">${modelName}</div>
                    </div>
                    <div class="card-body">
                        <div class="info-group">
                            <label>Mức pin</label>
                            <div class="value battery-value ${batteryClass}">${vehicle.battery || 0}%</div>
                        </div>
                        <div class="info-group">
                            <label>Trạng thái</label>
                            <span class="status-badge ${statusBadgeClass}">${statusText}</span>
                        </div>
                        ${issueSection}
                    </div>
                    <div class="card-footer">
                        <button class="btn-card" data-id="${vehicle.id}" data-action="update">Cập nhật</button>
                        <button class="btn-card primary" data-id="${vehicle.id}" data-action="report">Báo cáo sự cố</button>
                    </div>
                `;
                vehicleGrid.appendChild(card);
            });

            console.log(`Đã tải ${data.vehicles.length} xe thành công`);

        } catch (error) {
            console.error("Lỗi tải danh sách xe:", error);
            vehicleGrid.innerHTML = '<p style="color: #e74c3c;">Lỗi khi tải dữ liệu xe.</p>';
        }
    }

    let currentEditingVehicle = null;
    let currentReportingVehicle = null;

    vehicleGrid.addEventListener('click', (event) => {
        const target = event.target;
        if (target.tagName !== 'BUTTON' || !target.dataset.id) return;

        const vehicleId = target.dataset.id;
        const action = target.dataset.action;

        if (action === 'update') {
            openUpdateModal(vehicleId);
        } else if (action === 'report') {
            openReportModal(vehicleId);
        }
    });

    // Function to open report issue modal
    window.openReportModal = function(vehicleId) {
        try {
            // Find vehicle data from grid
            const vehicleCard = Array.from(vehicleGrid.querySelectorAll('.vehicle-card')).find(card => {
                return card.querySelector('button[data-id="' + vehicleId + '"]') !== null;
            });

            if (!vehicleCard) {
                console.error('Vehicle card not found');
                return;
            }

            // Extract plate from card
            const plateText = vehicleCard.querySelector('.plate').textContent;

            // Fill modal with current data
            document.getElementById('reportModalPlate').value = plateText;
            document.getElementById('issueDescription').value = '';
            document.getElementById('issueSeverity').value = 'MODERATE';

            // Store vehicle info for reporting
            currentReportingVehicle = {
                id: vehicleId,
                plate: plateText
            };

            // Show modal
            document.getElementById('reportIssueModal').style.display = 'block';
        } catch (error) {
            console.error('Error opening report modal:', error);
            alert('Lỗi khi mở modal báo cáo sự cố');
        }
    };

    // Function to close report modal
    window.closeReportModal = function() {
        document.getElementById('reportIssueModal').style.display = 'none';
        currentReportingVehicle = null;
    };

    // Function to submit issue report
    window.submitIssueReport = async function() {
        if (!currentReportingVehicle) {
            alert('Lỗi: Không tìm thấy thông tin xe');
            return;
        }

        const issueDescription = document.getElementById('issueDescription').value.trim();
        const issueSeverity = document.getElementById('issueSeverity').value;

        if (!issueDescription) {
            alert('Vui lòng nhập mô tả sự cố');
            return;
        }

        try {
            console.log('Reporting issue for vehicle:', {
                id: currentReportingVehicle.id,
                issue: issueDescription,
                severity: issueSeverity
            });

            // Make API call to report issue and update vehicle status to MAINTENANCE
            const response = await fetch(`/api/vehicles/${currentReportingVehicle.id}/report-issue`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    issue: issueDescription,
                    severity: issueSeverity
                })
            });

            if (!response.ok) {
                const errorData = await response.text();
                throw new Error(`Báo cáo thất bại (${response.status}): ${errorData}`);
            }

            const result = await response.json();
            console.log('Report submitted successfully:', result);

            alert('Báo cáo sự cố thành công! Xe được chuyển sang trạng thái bảo trì.');
            closeReportModal();

            // Reload vehicles list
            loadVehiclesForStation();
        } catch (error) {
            console.error('Error submitting issue report:', error);
            alert('Lỗi khi báo cáo: ' + error.message);
        }
    };

    // Close modal when clicking outside of it
    window.onClickOutsideModal = function(event) {
        const updateModal = document.getElementById('updateVehicleModal');
        const reportModal = document.getElementById('reportIssueModal');
        if (event.target == updateModal) {
            updateModal.style.display = 'none';
            currentEditingVehicle = null;
        }
        if (event.target == reportModal) {
            reportModal.style.display = 'none';
            currentReportingVehicle = null;
        }
    };
    // Function to open update modal
    window.openUpdateModal = function(vehicleId) {
        try {
            // Find vehicle data from grid
            const vehicleCard = Array.from(vehicleGrid.querySelectorAll('.vehicle-card')).find(card => {
                return card.querySelector('button[data-id="' + vehicleId + '"]') !== null;
            });

            if (!vehicleCard) {
                console.error('Vehicle card not found');
                return;
            }

            // Extract data from card
            const plateText = vehicleCard.querySelector('.plate').textContent;
            const batteryText = vehicleCard.querySelector('.battery-value').textContent;
            const battery = parseInt(batteryText);

            // Get current booking status from badge text
            const badgeText = vehicleCard.querySelector('.status-badge').textContent;
            const statusMap = {
                'Chờ thanh toán': 'PENDING_PAYMENT',
                'Sẵn sàng': 'AVAILABLE',
                'Chưa sẵn sàng': 'MAINTENANCE',
                'Đang thuê': 'RENTED'
            };
            const currentStatus = statusMap[badgeText] || 'AVAILABLE';

            // Fill modal with current data
            document.getElementById('modalPlate').value = plateText;
            document.getElementById('modalBattery').value = battery;
            document.getElementById('modalBookingStatus').value = currentStatus;

            // Store vehicle info for saving
            currentEditingVehicle = {
                id: vehicleId,
                plate: plateText
            };

            // Show modal
            document.getElementById('updateVehicleModal').style.display = 'block';
        } catch (error) {
            console.error('Error opening modal:', error);
            alert('Lỗi khi mở modal cập nhật');
        }
    };

    // Function to close modal
    window.closeUpdateModal = function() {
        document.getElementById('updateVehicleModal').style.display = 'none';
        currentEditingVehicle = null;
    };

    // Function to save vehicle update
    window.saveVehicleUpdate = async function() {
        if (!currentEditingVehicle) {
            alert('Lỗi: Không tìm thấy thông tin xe');
            return;
        }

        try {
            const battery = parseInt(document.getElementById('modalBattery').value);
            const bookingStatus = document.getElementById('modalBookingStatus').value;

            // Validate input
            if (isNaN(battery) || battery < 0 || battery > 100) {
                alert('Mức pin phải từ 0 đến 100%');
                return;
            }

            console.log('Updating vehicle:', {
                id: currentEditingVehicle.id,
                battery: battery,
                bookingStatus: bookingStatus
            });

            // Make API call to update vehicle
            const response = await fetch(`/api/vehicles/${currentEditingVehicle.id}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    battery: battery,
                    bookingStatus: bookingStatus
                })
            });

            if (!response.ok) {
                const errorData = await response.text();
                throw new Error(`Cập nhật thất bại (${response.status}): ${errorData}`);
            }

            const result = await response.json();
            console.log('Update successful:', result);

            alert('Cập nhật xe thành công!');
            closeUpdateModal();

            // Reload vehicles list
            loadVehiclesForStation();
        } catch (error) {
            console.error('Error saving vehicle update:', error);
            alert('Lỗi khi cập nhật: ' + error.message);
        }
    };
});