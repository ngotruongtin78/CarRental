document.addEventListener("DOMContentLoaded", function() {
    let allReports = [];
    let currentReport = null;

    // Xử lý menu Admin
    window.toggleProfileMenu = function(event) {
        event.stopPropagation();
        const dropdown = document.getElementById('profileDropdown');
        if (dropdown) dropdown.classList.toggle('show');
    };
    window.addEventListener('click', function(event) {
        if (!event.target.closest('.admin-profile')) {
            const dropdown = document.getElementById('profileDropdown');
            if (dropdown) dropdown.classList.remove('show');
        }
        // Đóng modal khi click ngoài
        if (event.target == document.getElementById('reportDetailModal')) {
            closeDetailModal();
        }
    });

    loadReports();

    async function loadReports() {
        try {
            const res = await fetch('/api/vehicle-reports/all');
            if (!res.ok) throw new Error("Lỗi tải dữ liệu");
            const data = await res.json();
            allReports = data.reports || [];

            // Sắp xếp mới nhất lên đầu
            allReports.sort((a, b) => new Date(b.reportedDate) - new Date(a.reportedDate));

            renderReports(allReports);
        } catch (err) {
            console.error(err);
            document.getElementById('reportsContainer').innerHTML = '<p style="text-align:center; color:red">Không thể tải danh sách báo cáo.</p>';
        }
    }

    function renderReports(reports) {
        const container = document.getElementById('reportsContainer');
        container.innerHTML = '';

        if (reports.length === 0) {
            container.innerHTML = '<div class="empty-state" style="text-align:center; width:100%; padding:40px; color:#999;">📭 Không có sự cố nào phù hợp.</div>';
            return;
        }

        reports.forEach(r => {
            const card = document.createElement('div');
            card.className = `report-card severity-${r.severity ? r.severity.toLowerCase() : 'minor'}`;

            const dateStr = r.reportedDate ? new Date(r.reportedDate).toLocaleDateString('vi-VN') : 'N/A';

            let statusLabel = '';
            if(r.status === 'REPORTED') statusLabel = '<span class="status-badge status-pending" style="background:#fef3e6; color:#f39c12; padding:4px 8px; border-radius:10px; font-size:12px;">Mới báo cáo</span>';
            else if(r.status === 'IN_REPAIR') statusLabel = '<span class="status-badge status-rented" style="background:#eaf3fb; color:#3498db; padding:4px 8px; border-radius:10px; font-size:12px;">Đang sửa</span>';
            else statusLabel = '<span class="status-badge status-available" style="background:#e6f7f0; color:#2ecc71; padding:4px 8px; border-radius:10px; font-size:12px;">Đã xong</span>';

            let severityLabel = r.severity;
            if(r.severity === 'CRITICAL') severityLabel = '🔴 Nghiêm trọng';
            else if(r.severity === 'MODERATE') severityLabel = '🟠 Trung bình';
            else severityLabel = '🟡 Nhẹ';

            card.innerHTML = `
                <div class="report-card-header">
                    <div class="report-plate">${r.vehiclePlate}</div>
                    <div class="report-date" style="font-size:12px; color:#777;">${dateStr}</div>
                </div>
                <div class="report-card-body">
                    <div class="report-field" style="margin-bottom:10px;">
                        <div class="report-value" style="display:flex; justify-content:space-between; align-items:center;">
                            ${statusLabel}
                            <span style="font-size:12px; font-weight:600;">${severityLabel}</span>
                        </div>
                    </div>
                    <div class="report-field" style="margin-bottom:10px;">
                        <div class="report-label" style="font-size:11px; color:#888; text-transform:uppercase;">Sự cố</div>
                        <div class="report-value" style="font-weight:600; color:#333;">${r.issue}</div>
                    </div>
                    <div class="report-field">
                        <div class="report-label" style="font-size:11px; color:#888; text-transform:uppercase;">Nhân viên báo</div>
                        <div class="report-value">${r.staffName || r.staffId}</div>
                    </div>
                </div>
                <div class="report-card-footer">
                    <button class="btn-small btn-small-primary" onclick="openDetail('${r.id}')" style="width:100%">Xử lý / Chi tiết</button>
                </div>
            `;
            container.appendChild(card);
        });
    }

    window.openDetail = function(id) {
        currentReport = allReports.find(r => r.id === id);
        if(!currentReport) return;

        const modal = document.getElementById('reportDetailModal');
        const content = document.getElementById('reportDetailContent');

        content.innerHTML = `
            <div class="form-group" style="margin-bottom:15px;">
                <label style="display:block; font-weight:600; margin-bottom:5px;">Biển số:</label>
                <input type="text" value="${currentReport.vehiclePlate}" disabled style="width:100%; padding:8px; border:1px solid #ddd; border-radius:4px; background:#f9f9f9;">
            </div>
            <div class="form-group" style="margin-bottom:15px;">
                <label style="display:block; font-weight:600; margin-bottom:5px;">Mô tả sự cố:</label>
                <textarea rows="3" disabled style="width:100%; padding:8px; border:1px solid #ddd; border-radius:4px; background:#f9f9f9;">${currentReport.issue}</textarea>
            </div>
            <div class="form-group">
                <label style="display:block; font-weight:600; margin-bottom:5px;">Ghi chú xử lý / Kết quả:</label>
                <textarea id="resolveNotes" rows="4" placeholder="Nhập ghi chú sửa chữa..." style="width:100%; padding:8px; border:1px solid #ddd; border-radius:4px;">${currentReport.notes || ''}</textarea>
            </div>
        `;

        modal.style.display = 'block';
    };

    window.closeDetailModal = function() {
        document.getElementById('reportDetailModal').style.display = 'none';
    };

    window.markAsResolved = async function() {
        if(!currentReport) return;

        const notes = document.getElementById('resolveNotes').value;

        if(!confirm("Xác nhận xe đã sửa xong và sẵn sàng hoạt động trở lại?")) return;

        try {
            // 1. Lưu ghi chú
            await fetch(`/api/vehicle-reports/${currentReport.id}/notes`, {
                method: 'PUT',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({ notes: notes })
            });

            // 2. Cập nhật trạng thái -> RESOLVED
            const res = await fetch(`/api/vehicle-reports/${currentReport.id}/status`, {
                method: 'PUT',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({ status: 'RESOLVED' })
            });

            if(res.ok) {
                alert("Cập nhật thành công! Xe đã chuyển sang trạng thái SẴN SÀNG.");
                closeDetailModal();
                loadReports(); // Tải lại danh sách
            } else {
                alert("Lỗi cập nhật.");
            }
        } catch(e) {
            console.error(e);
            alert("Lỗi kết nối.");
        }
    };

    // Filter Logic
    function filter() {
        const st = document.getElementById('statusFilter').value;
        const sv = document.getElementById('severityFilter').value;

        const filtered = allReports.filter(r => {
            return (!st || r.status === st) && (!sv || r.severity === sv);
        });
        renderReports(filtered);
    }

    document.getElementById('statusFilter').addEventListener('change', filter);
    document.getElementById('severityFilter').addEventListener('change', filter);
});