async function loadRatings() {
    const tbody = document.getElementById('ratings-body');
    tbody.innerHTML = '<tr><td colspan="6" class="text-center">Đang tải...</td></tr>';

    try {
        const res = await fetch('/api/ratings/admin');
        if (!res.ok) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center">Không tải được dữ liệu.</td></tr>';
            return;
        }
        const data = await res.json();
        document.getElementById('rating-count').innerText = `${data.length} đánh giá`;

        if (!data.length) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center">Chưa có đánh giá nào.</td></tr>';
            return;
        }

        tbody.innerHTML = '';
        data.forEach(item => {
            const tr = document.createElement('tr');
            const created = item.createdAt ? new Date(item.createdAt) : null;
            const createdText = created ? created.toLocaleString('vi-VN') : '-';
            tr.innerHTML = `
                <td>${createdText}</td>
                <td>${item.username || item.userId || '-'}</td>
                <td>${item.vehiclePlate || item.vehicleId || '-'}</td>
                <td><strong>${item.vehicleScore || 0}/5</strong></td>
                <td><strong>${item.staffScore || 0}/5</strong></td>
                <td>${item.comment ? item.comment : ''}</td>
            `;
            tbody.appendChild(tr);
        });
    } catch (err) {
        console.error('Lỗi tải đánh giá', err);
        tbody.innerHTML = '<tr><td colspan="6" class="text-center">Đã xảy ra lỗi.</td></tr>';
    }
}

document.addEventListener('DOMContentLoaded', loadRatings);
