// Review modal state
const reviewModalState = {
    el: null,
    carRatingEl: null,
    staffRatingEl: null,
    commentEl: null,
    carRatingValueEl: null,
    staffRatingValueEl: null,
    bookingIdEl: null,
    carIdEl: null,
    staffIdEl: null
};

let reviewedBookings = new Set();

function initReviewModal() {
    reviewModalState.el = document.getElementById('review-modal');
    reviewModalState.carRatingEl = document.getElementById('car-rating');
    reviewModalState.staffRatingEl = document.getElementById('staff-rating');
    reviewModalState.commentEl = document.getElementById('review-comment');
    reviewModalState.carRatingValueEl = document.getElementById('car-rating-value');
    reviewModalState.staffRatingValueEl = document.getElementById('staff-rating-value');
    reviewModalState.bookingIdEl = document.getElementById('review-booking-id');
    reviewModalState.carIdEl = document.getElementById('review-car-id');
    reviewModalState.staffIdEl = document.getElementById('review-staff-id');
    
    // Setup star rating click handlers
    setupStarRating('car-rating', 'car-rating-value');
    setupStarRating('staff-rating', 'staff-rating-value');
    
    // Setup modal controls
    document.getElementById('btn-review-submit')?.addEventListener('click', submitReview);
    document.getElementById('btn-review-cancel')?.addEventListener('click', closeReviewModal);
    reviewModalState.el?.querySelector('.dialog-overlay')?.addEventListener('click', closeReviewModal);
    reviewModalState.el?.querySelector('.dialog-close')?.addEventListener('click', closeReviewModal);
}

function setupStarRating(containerId, valueInputId) {
    const container = document.getElementById(containerId);
    const valueInput = document.getElementById(valueInputId);
    if (!container || !valueInput) return;
    
    const stars = container.querySelectorAll('i');
    
    stars.forEach((star, index) => {
        // Hover effects
        star.addEventListener('mouseenter', () => {
            stars.forEach((s, i) => {
                if (i <= index) {
                    s.classList.add('hover');
                } else {
                    s.classList.remove('hover');
                }
            });
        });
        
        star.addEventListener('mouseleave', () => {
            stars.forEach(s => s.classList.remove('hover'));
        });
        
        // Click to set rating
        star.addEventListener('click', () => {
            const rating = parseInt(star.getAttribute('data-rating'));
            valueInput.value = rating;
            
            stars.forEach((s, i) => {
                if (i < rating) {
                    s.classList.add('active');
                } else {
                    s.classList.remove('active');
                }
            });
        });
    });
}

function openReviewModal(record) {
    if (!record || !reviewModalState.el) return;
    
    // Reset the modal
    resetReviewModal();
    
    // Set hidden values
    if (reviewModalState.bookingIdEl) reviewModalState.bookingIdEl.value = record.id || '';
    if (reviewModalState.carIdEl) reviewModalState.carIdEl.value = record.vehicleId || '';
    if (reviewModalState.staffIdEl) reviewModalState.staffIdEl.value = record.deliveryStaffId || record.returnStaffId || '';
    
    // Show modal
    reviewModalState.el.classList.add('show');
}

function closeReviewModal() {
    reviewModalState.el?.classList.remove('show');
}

function resetReviewModal() {
    // Reset star ratings
    ['car-rating', 'staff-rating'].forEach(id => {
        const container = document.getElementById(id);
        if (container) {
            container.querySelectorAll('i').forEach(s => s.classList.remove('active', 'hover'));
        }
    });
    
    // Reset values
    if (reviewModalState.carRatingValueEl) reviewModalState.carRatingValueEl.value = '0';
    if (reviewModalState.staffRatingValueEl) reviewModalState.staffRatingValueEl.value = '0';
    if (reviewModalState.commentEl) reviewModalState.commentEl.value = '';
    if (reviewModalState.bookingIdEl) reviewModalState.bookingIdEl.value = '';
    if (reviewModalState.carIdEl) reviewModalState.carIdEl.value = '';
    if (reviewModalState.staffIdEl) reviewModalState.staffIdEl.value = '';
}

async function submitReview() {
    const bookingId = reviewModalState.bookingIdEl?.value;
    const carId = reviewModalState.carIdEl?.value;
    const staffId = reviewModalState.staffIdEl?.value;
    const carRating = parseInt(reviewModalState.carRatingValueEl?.value || '0');
    const staffRating = parseInt(reviewModalState.staffRatingValueEl?.value || '0');
    const comment = reviewModalState.commentEl?.value?.trim() || '';
    
    if (!bookingId) {
        alert('Không xác định được mã chuyến thuê.');
        return;
    }
    
    if (carRating === 0 && staffRating === 0) {
        alert('Vui lòng đánh giá ít nhất một mục (xe hoặc nhân viên).');
        return;
    }
    
    try {
        const res = await fetch('/api/reviews', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                bookingId,
                carId,
                staffId,
                carRating: carRating > 0 ? carRating : null,
                staffRating: staffRating > 0 ? staffRating : null,
                comment
            })
        });
        
        if (!res.ok) {
            const errorText = await res.text();
            alert(errorText || 'Không thể gửi đánh giá. Vui lòng thử lại.');
            return;
        }
        
        alert('Cảm ơn bạn đã đánh giá!');
        reviewedBookings.add(bookingId);
        closeReviewModal();
        
        // Refresh the history list if available
        if (typeof loadHistory === 'function') {
            loadHistory();
        }
    } catch (e) {
        console.error('Error submitting review:', e);
        alert('Lỗi kết nối. Vui lòng thử lại.');
    }
}

async function checkIfReviewed(bookingId) {
    if (reviewedBookings.has(bookingId)) return true;
    
    try {
        const res = await fetch(`/api/reviews/check/${bookingId}`);
        if (res.ok) {
            const data = await res.json();
            if (data.reviewed) {
                reviewedBookings.add(bookingId);
                return true;
            }
        }
        return false;
    } catch (e) {
        console.error('Error checking review status:', e);
        return false;
    }
}

// Function to add review button to completed bookings
async function addReviewButton(container, record, vehicle, station) {
    if (!record || !container) return;
    
    const status = (record.status || '').toUpperCase();
    if (status !== 'COMPLETED') return;
    
    const isReviewed = await checkIfReviewed(record.id);
    
    const reviewContainer = document.createElement('div');
    reviewContainer.className = 'review-action';
    reviewContainer.style.marginTop = '10px';
    
    if (isReviewed) {
        reviewContainer.innerHTML = `
            <span class="reviewed-badge">
                <i class="fas fa-check-circle"></i> Đã đánh giá
            </span>
        `;
    } else {
        const btn = document.createElement('button');
        btn.className = 'btn-review';
        btn.innerHTML = '<i class="fas fa-star"></i> Đánh giá';
        btn.onclick = (e) => {
            e.stopPropagation();
            openReviewModal({
                id: record.id,
                vehicleId: record.vehicleId,
                deliveryStaffId: record.deliveryStaffId,
                returnStaffId: record.returnStaffId
            });
        };
        reviewContainer.appendChild(btn);
    }
    
    container.appendChild(reviewContainer);
}

// Initialize when DOM is ready
window.addEventListener('DOMContentLoaded', () => {
    initReviewModal();
});

// Export for use in lichsuthue.js
window.addReviewButton = addReviewButton;
window.openReviewModal = openReviewModal;
