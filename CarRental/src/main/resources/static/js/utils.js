/**
 * Utility functions dùng chung cho toàn bộ frontend
 */

// Format ngày tháng
function formatDate(dateStr) {
    if (!dateStr) return "";
    const d = new Date(dateStr);
    if (isNaN(d.getTime())) return "";
    return d.toLocaleDateString("vi-VN");
}

// Format ngày giờ
function formatDateTime(dateStr) {
    if (!dateStr) return "";
    const d = new Date(dateStr);
    if (isNaN(d.getTime())) return "";
    return d.toLocaleString("vi-VN", { 
        year: 'numeric', 
        month: '2-digit', 
        day: '2-digit',
        hour: '2-digit', 
        minute: '2-digit' 
    });
}

// Format tiền tệ
function formatMoney(value) {
    if (value === undefined || value === null || isNaN(value)) return "0 ₫";
    return Number(value).toLocaleString("vi-VN") + " ₫";
}

// Format tọa độ
function formatCoords(lat, lon) {
    if (typeof lat !== "number" || typeof lon !== "number") return "";
    return `${lat.toFixed(5)}, ${lon.toFixed(5)}`;
}

// Convert base64 to Blob (dùng cho upload ảnh)
function base64ToBlob(base64Data) {
    try {
        const base64 = base64Data.split(',')[1];
        const binaryString = atob(base64);
        const bytes = new Uint8Array(binaryString.length);
        for (let i = 0; i < binaryString.length; i++) {
            bytes[i] = binaryString.charCodeAt(i);
        }
        return new Blob([bytes], { type: 'image/png' });
    } catch (e) {
        console.error('Error converting base64 to blob:', e);
        return null;
    }
}

// Hiển thị toast notification
function showToast(message, type = 'info') {
    const toast = document.getElementById('toast');
    if (!toast) return;
    
    toast.textContent = message;
    toast.className = `toast show ${type}`;
    
    setTimeout(() => {
        toast.className = 'toast hidden';
    }, 3000);
}
