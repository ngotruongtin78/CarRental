document.addEventListener("DOMContentLoaded", function() {

    const ctxRevenue = document.getElementById('revenueChart')?.getContext('2d');
    const ctxPeakHours = document.getElementById('peakHoursChart')?.getContext('2d');
    const kpiRevenueEl = document.getElementById('kpi-revenue');
    const kpiRevenueChangeEl = document.getElementById('kpi-revenue-change');
    const kpiTripsEl = document.getElementById('kpi-trips');
    const kpiUtilizationEl = document.getElementById('kpi-utilization');
    let revenueChartInstance = null;
    let peakHoursChartInstance = null;
    async function loadKpiData() {
        try {
            const kpiData = {
                totalRevenue: 850000000,
                revenueChange: 0.12,
                totalTrips: 15000,
                avgUtilization: 0.78
            };
            kpiRevenueEl.textContent = kpiData.totalRevenue.toLocaleString('vi-VN') + 'đ';
            kpiRevenueChangeEl.textContent = `${kpiData.revenueChange > 0 ? '+' : ''}${(kpiData.revenueChange * 100).toFixed(0)}% so với tháng trước`;
            kpiRevenueChangeEl.className = `change ${kpiData.revenueChange > 0 ? 'positive' : 'negative'}`;
            kpiTripsEl.textContent = kpiData.totalTrips.toLocaleString('vi-VN');
            kpiUtilizationEl.textContent = (kpiData.avgUtilization * 100).toFixed(0) + '%';

        } catch (error) {
            console.error('Lỗi tải dữ liệu KPI:', error);
        }
    }

    async function loadRevenueChart() {
        if (!ctxRevenue) return;

        try {
            const chartData = {
                labels: ['70', '95', '100', '115', '180', '210', '280'],
                values: [2, 9, 7, 28, 25, 35, 40]
            };

            if (revenueChartInstance) {
                revenueChartInstance.destroy();
            }

            revenueChartInstance = new Chart(ctxRevenue, {
                type: 'line',
                data: {
                    labels: chartData.labels,
                    datasets: [{
                        label: 'Doanh thu (triệu)',
                        data: chartData.values,
                        borderColor: '#3498db',
                        backgroundColor: 'rgba(52, 152, 219, 0.1)',
                        fill: true,
                        tension: 0.3
                    }]
                },
                options: {
                    responsive: true,

                    maintainAspectRatio: true
                }
            });
        } catch (error) {
            console.error('Lỗi tải biểu đồ doanh thu:', error);
        }
    }

    async function loadPeakHoursChart() {
        if (!ctxPeakHours) return;

        try {
            const peakData = {
                labels: ['7h', '5h', '5.5h', '6h', '6.5h', '7h', '7.5h'],
                values: [5.0, 7.0, 7.0, 5.9, 1.0, 0.0, 16.0]
            };

            if (peakHoursChartInstance) {
                peakHoursChartInstance.destroy();
            }

            peakHoursChartInstance = new Chart(ctxPeakHours, {
                type: 'bar',
                data: {
                    labels: peakData.labels,
                    datasets: [{
                        label: '% số chuyến',
                        data: peakData.values,
                        backgroundColor: '#3498db',
                        borderRadius: 4
                    }]
                },
                options: {
                    responsive: true,
                    // === SỬA DÒNG NÀY ===
                    maintainAspectRatio: true,
                    plugins: {
                        legend: {
                            display: false
                        }
                    },
                    scales: {
                        y: {
                            beginAtZero: true,
                            ticks: {
                                callback: function(value) {
                                    return value + '%';
                                }
                            }
                        }
                    }
                }
            });
        } catch (error) {
            console.error('Lỗi tải biểu đồ giờ cao điểm:', error);
        }
    }

    loadKpiData();
    loadRevenueChart();
    loadPeakHoursChart();
});