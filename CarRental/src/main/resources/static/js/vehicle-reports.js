document.addEventListener("DOMContentLoaded", function() {
    let allReports = [];
    let currentDetailReport = null;

    loadReports();

    async function loadReports() {
        try {
            const response = await fetch('/api/vehicle-reports/all');
            if (!response.ok) {
                throw new Error('Lỗi khi tải báo cáo');
            }

            const data = await response.json();
            allReports = data.reports || [];

            console.log('Báo cáo được tải:', allReports);

            updateStatistics();
            renderReports(allReports);
        } catch (error) {
            console.error('Lỗi:', error);
            document.getElementById('reportsContainer').innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">⚠️</div>
                    <p>Lỗi khi tải báo cáo: ${error.message}</p>
                </div>
            `;
        }
    }

    function updateStatistics() {
        const reported = allReports.filter(r => r.status === 'REPORTED').length;
        const inRepair = allReports.filter(r => r.status === 'IN_REPAIR').length;
        const resolved = allReports.filter(r => r.status === 'RESOLVED').length;

        document.getElementById('reportedCount').textContent = reported;
        document.getElementById('inRepairCount').textContent = inRepair;
        document.getElementById('resolvedCount').textContent = resolved;
    }

    function renderReports(reports) {
        const container = document.getElementById('reportsContainer');

        if (!reports || reports.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">📭</div>
                    <p>Không có báo cáo sự cố</p>
                </div>
            `;
            return;
        }

        container.innerHTML = '';

        reports.forEach(report => {
            const card = createReportCard(report);
            container.appendChild(card);
        });
    }

    function createReportCard(report) {
        const card = document.createElement('div');
        card.className = `report-card severity-${report.severity.toLowerCase()}`;

        const formattedDate = new Date(report.reportedDate).toLocaleDateString('vi-VN', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        });

        const severityText = getSeverityText(report.severity);
        const severityBadge = `<span class="report-severity severity-${report.severity.toLowerCase()}-badge">${severityText}</span>`;

        const statusText = getStatusText(report.status);
        const statusClass = `status-${report.status.toLowerCase().replace('_', '-')}`;
        const statusBadge = `<span class="report-status ${statusClass}">${statusText}</span>`;

        card.innerHTML = `
            <div class="report-card-header">
                <div class="report-plate">${report.vehiclePlate}</div>
                <div class="report-date">${formattedDate}</div>
            </div>
            <div class="report-card-body">
                <div class="report-field">
                    <div class="report-label">Mức độ / Trạng thái</div>
                    <div class="report-value">
                        ${severityBadge}
                        ${statusBadge}
                    </div>
                </div>
                <div class="report-field">
                    <div class="report-label">Sự cố</div>
                    <div class="report-value">${report.issue}</div>
                </div>
                <div class="report-field">
                    <div class="report-label">Báo cáo bởi</div>
                    <div class="report-value">${report.staffName || report.staffId}</div>
                </div>
                ${report.notes ? `
                <div class="report-field">
                    <div class="report-label">Ghi chú</div>
                    <div class="report-value">${report.notes}</div>
                </div>
                ` : ''}
            </div>
            <div class="report-card-footer">
                <button class="btn-small btn-small-primary" onclick="viewReportDetail('${report.id}')">Chi tiết</button>
                <button class="btn-small btn-small-danger" onclick="deleteReport('${report.id}')">Xóa</button>
            </div>
        `;

        return card;
    }

    function getSeverityText(severity) {
        const severityMap = {
            'MINOR': 'Nhẹ',
            'MODERATE': 'Trung bình',
            'CRITICAL': 'Nghiêm trọng'
        };
        return severityMap[severity] || severity;
    }

    function getStatusText(status) {
        const statusMap = {
            'REPORTED': 'Đã báo cáo',
            'IN_REPAIR': 'Đang sửa chữa',
            'RESOLVED': 'Đã khắc phục'
        };
        return statusMap[status] || status;
    }

    window.filterReports = function() {
        const statusFilter = document.getElementById('statusFilter').value;
        const severityFilter = document.getElementById('severityFilter').value;

        let filtered = allReports;

        if (statusFilter) {
            filtered = filtered.filter(r => r.status === statusFilter);
        }

        if (severityFilter) {
            filtered = filtered.filter(r => r.severity === severityFilter);
        }

        renderReports(filtered);
    };

    window.resetFilters = function() {
        document.getElementById('statusFilter').value = '';
        document.getElementById('severityFilter').value = '';
        renderReports(allReports);
    };

    window.viewReportDetail = function(reportId) {
        const report = allReports.find(r => r.id === reportId);
        if (!report) {
            alert('Không tìm thấy báo cáo');
            return;
        }

        currentDetailReport = report;
        const formattedDate = new Date(report.reportedDate).toLocaleDateString('vi-VN', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        });

        const detailContent = `
            <div class="form-group">
                <label>Biển số xe:</label>
                <input type="text" value="${report.vehiclePlate}" disabled>
            </div>
            <div class="form-group">
                <label>ID Xe:</label>
                <input type="text" value="${report.vehicleId}" disabled>
            </div>
            <div class="form-group">
                <label>Mô tả sự cố:</label>
                <textarea rows="4" disabled>${report.issue}</textarea>
            </div>
            <div class="form-group">
                <label>Mức độ nghiêm trọng:</label>
                <input type="text" value="${getSeverityText(report.severity)}" disabled>
            </div>
            <div class="form-group">
                <label>Trạng thái:</label>
                <input type="text" value="${getStatusText(report.status)}" disabled>
            </div>
            <div class="form-group">
                <label>Báo cáo bởi:</label>
                <input type="text" value="${report.staffName || report.staffId}" disabled>
            </div>
            <div class="form-group">
                <label>Ngày báo cáo:</label>
                <input type="text" value="${formattedDate}" disabled>
            </div>
            <div class="form-group">
                <label>Ghi chú / Kết quả sửa chữa:</label>
                <textarea id="notesInput" rows="4" placeholder="Nhập ghi chú hoặc kết quả sửa chữa...">${report.notes || ''}</textarea>
            </div>
            <div class="form-group">
                <label>📸 Chụp hình báo cáo:</label>
                <div style="border: 2px dashed #ccc; border-radius: 6px; padding: 15px; text-align: center; background-color: #f9f9f9;">
                    <input type="file" id="photoInput" accept="image/*" style="display: none;">
                    <button type="button" class="btn btn-small btn-small-primary" onclick="document.getElementById('photoInput').click()" style="width: auto; padding: 10px 20px;">
                        📷 Chọn ảnh từ máy
                    </button>
                    <div id="photoPreview" style="margin-top: 10px; display: none;">
                        <img id="previewImg" src="" style="max-width: 100%; max-height: 200px; border-radius: 4px;">
                        <p id="photoFileName" style="font-size: 12px; color: #666; margin-top: 5px;"></p>
                    </div>
                </div>
            </div>
        `;

        document.getElementById('reportDetailContent').innerHTML = detailContent;

        // Setup photo input listener
        setTimeout(() => {
            const photoInput = document.getElementById('photoInput');
            if (photoInput) {
                photoInput.addEventListener('change', handlePhotoSelect);
            }
        }, 100);

        document.getElementById('reportDetailModal').classList.add('show');
    };

    function handlePhotoSelect(event) {
        const file = event.target.files[0];
        if (!file) return;

        const reader = new FileReader();
        reader.onload = function(e) {
            const preview = document.getElementById('photoPreview');
            const previewImg = document.getElementById('previewImg');
            const photoFileName = document.getElementById('photoFileName');

            previewImg.src = e.target.result;
            photoFileName.textContent = file.name;
            preview.style.display = 'block';

            // Store base64 for later use
            window.currentPhotoBase64 = e.target.result;
            window.currentPhotoFileName = file.name;
        };
        reader.readAsDataURL(file);
    }

    window.closeReportDetailModal = function() {
        document.getElementById('reportDetailModal').classList.remove('show');
        currentDetailReport = null;
    };

    window.markAsResolved = async function() {
        if (!currentDetailReport) {
            alert('Không tìm thấy báo cáo');
            return;
        }

        const notes = document.getElementById('notesInput').value;
        const photoBase64 = window.currentPhotoBase64;

        try {
            // Cập nhật ghi chú
            if (notes.trim()) {
                const notesResponse = await fetch(`/api/vehicle-reports/${currentDetailReport.id}/notes`, {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({ notes: notes })
                });

                if (!notesResponse.ok) {
                    throw new Error('Lỗi khi lưu ghi chú');
                }
            }

            // Cập nhật ảnh báo cáo vào RentalRecord
            if (photoBase64) {
                // Convert base64 to binary
                const binaryString = atob(photoBase64.split(',')[1]);
                const bytes = new Uint8Array(binaryString.length);
                for (let i = 0; i < binaryString.length; i++) {
                    bytes[i] = binaryString.charCodeAt(i);
                }

                // Gửi binary data lên server
                const photoResponse = await fetch(`/api/vehicle-reports/${currentDetailReport.id}/photo`, {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/octet-stream',
                        'X-Photo-Name': window.currentPhotoFileName || 'report-photo'
                    },
                    body: bytes.buffer
                });

                if (!photoResponse.ok) {
                    throw new Error('Lỗi khi lưu ảnh');
                }
            }

            // Cập nhật trạng thái báo cáo thành RESOLVED
            const statusResponse = await fetch(`/api/vehicle-reports/${currentDetailReport.id}/status`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ status: 'RESOLVED' })
            });

            if (!statusResponse.ok) {
                throw new Error('Lỗi khi cập nhật trạng thái báo cáo');
            }

            // Cập nhật trạng thái xe thành AVAILABLE và xóa sự cố
            const vehicleUpdateResponse = await fetch(`/api/vehicles/${currentDetailReport.vehicleId}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    available: true,
                    issue: null,
                    issueSeverity: null
                })
            });

            if (!vehicleUpdateResponse.ok) {
                console.warn('Cảnh báo: Lỗi khi cập nhật trạng thái xe, nhưng báo cáo đã được cập nhật');
            }

            alert('Cập nhật báo cáo thành công! Xe đã được chuyển về sẵn sàng.');
            closeReportDetailModal();
            loadReports();
        } catch (error) {
            console.error('Lỗi:', error);
            alert('Lỗi: ' + error.message);
        }
    };

    window.deleteReport = async function(reportId) {
        if (!confirm('Bạn chắc chắn muốn xóa báo cáo này?')) {
            return;
        }

        try {
            const response = await fetch(`/api/vehicle-reports/${reportId}`, {
                method: 'DELETE'
            });

            if (!response.ok) {
                throw new Error('Lỗi khi xóa báo cáo');
            }

            alert('Xóa báo cáo thành công!');
            loadReports();
        } catch (error) {
            console.error('Lỗi:', error);
            alert('Lỗi: ' + error.message);
        }
    };

    // Add event listeners for filters
    document.getElementById('statusFilter').addEventListener('change', filterReports);
    document.getElementById('severityFilter').addEventListener('change', filterReports);

    // Close modal when clicking outside
    window.onclick = function(event) {
        const modal = document.getElementById('reportDetailModal');
        if (event.target === modal) {
            closeReportDetailModal();
        }
    };
});

