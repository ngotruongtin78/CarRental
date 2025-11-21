document.addEventListener("DOMContentLoaded", function() {

    const ctxRevenue = document.getElementById('revenueChart')?.getContext('2d');
    const ctxPeakHours = document.getElementById('peakHoursChart')?.getContext('2d');
    const kpiRevenueEl = document.getElementById('kpi-revenue');
    const kpiTripsEl = document.getElementById('kpi-trips');
    const kpiUtilizationEl = document.getElementById('kpi-utilization');

    let revenueChartInstance = null;
    let peakHoursChartInstance = null;

    window.toggleProfileMenu = function(event) {
        event.stopPropagation();
        const dropdown = document.getElementById('profileDropdown');
        if (dropdown) dropdown.classList.toggle('show');
    };

    window.addEventListener('click', function(event) {
        if (!event.target.closest('.admin-profile')) {
            const dropdown = document.getElementById('profileDropdown');
            if (dropdown && dropdown.classList.contains('show')) {
                dropdown.classList.remove('show');
            }
        }
    });

    loadReportData();

    async function loadReportData() {
        try {
            const response = await fetch('/admin/reports/data');
            if (!response.ok) throw new Error("Không tải được dữ liệu báo cáo");

            const data = await response.json();

            if (kpiRevenueEl) {
                kpiRevenueEl.textContent = (data.totalRevenue || 0).toLocaleString('vi-VN') + 'đ';
            }
            if (kpiTripsEl) {
                kpiTripsEl.textContent = (data.totalTrips || 0).toLocaleString('vi-VN');
            }
            if (kpiUtilizationEl) {
                const util = data.avgUtilization ? (data.avgUtilization * 100).toFixed(1) : 0;
                kpiUtilizationEl.textContent = util + '%';
            }

            if (ctxRevenue) {
                if (revenueChartInstance) revenueChartInstance.destroy();

                revenueChartInstance = new Chart(ctxRevenue, {
                    type: 'bar',
                    data: {
                        labels: data.revenueChartLabels || [],
                        datasets: [{
                            label: 'Doanh thu (VNĐ)',
                            data: data.revenueChartValues || [],
                            backgroundColor: 'rgba(52, 152, 219, 0.6)',
                            borderColor: '#3498db',
                            borderWidth: 1,
                            borderRadius: 4,
                            barThickness: 40
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: true,
                        scales: {
                            y: {
                                beginAtZero: true,
                                ticks: {
                                    callback: function(value) {
                                        return value.toLocaleString('vi-VN');
                                    }
                                }
                            }
                        },
                        plugins: {
                            legend: {
                                display: false
                            }
                        }
                    }
                });
            }

            if (ctxPeakHours) {
                if (peakHoursChartInstance) peakHoursChartInstance.destroy();

                peakHoursChartInstance = new Chart(ctxPeakHours, {
                    type: 'bar',
                    data: {
                        labels: data.peakLabels || [],
                        datasets: [{
                            label: '% số chuyến',
                            data: data.peakValues || [],
                            backgroundColor: '#3498db',
                            borderRadius: 4
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: true,
                        plugins: {
                            legend: { display: false }
                        },
                        scales: {
                            y: {
                                beginAtZero: true,
                                ticks: {
                                    callback: function(value) { return value + '%'; }
                                }
                            }
                        }
                    }
                });
            }

        } catch (error) {
            console.error('Lỗi tải dữ liệu báo cáo:', error);
        }
    }
});