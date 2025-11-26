document.addEventListener("DOMContentLoaded", function() {

    // --- MENU PROFILE ---
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

    // --- KHỞI TẠO NGAY LẬP TỨC ---
    loadReportData();

    async function loadReportData() {
        try {
            const response = await fetch('/admin/reports/data');
            if (!response.ok) throw new Error("Lỗi HTTP: " + response.status);
            const data = await response.json();

            // 1. KPI
            if(document.getElementById('kpi-revenue'))
                document.getElementById('kpi-revenue').textContent = (data.totalRevenue || 0).toLocaleString('vi-VN') + 'đ';
            if(document.getElementById('kpi-trips'))
                document.getElementById('kpi-trips').textContent = (data.totalTrips || 0);
            if(document.getElementById('kpi-utilization'))
                document.getElementById('kpi-utilization').textContent = ((data.avgUtilization || 0) * 100).toFixed(1) + '%';

            // 2. BAR CHART (REVENUE)
            const revenueCanvas = document.getElementById('revenueChart');
            if (revenueCanvas) {
                let chartStatus = Chart.getChart(revenueCanvas);
                if (chartStatus != undefined) chartStatus.destroy();

                new Chart(revenueCanvas, {
                    type: 'bar',
                    data: {
                        labels: data.revenueChartLabels && data.revenueChartLabels.length > 0 ? data.revenueChartLabels : ['Chưa có'],
                        datasets: [{
                            label: 'Doanh thu (VNĐ)',
                            data: data.revenueChartValues && data.revenueChartValues.length > 0 ? data.revenueChartValues : [0],
                            backgroundColor: '#3498db',
                            hoverBackgroundColor: '#2980b9',
                            borderRadius: 4,
                            barPercentage: 0.6,
                            categoryPercentage: 0.8
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        plugins: { legend: { display: false } },
                        scales: {
                            y: {
                                beginAtZero: true,
                                ticks: {
                                    callback: function(value) {
                                        if (value >= 1000000) return (value / 1000000).toFixed(1) + 'M';
                                        if (value >= 1000) return (value / 1000).toFixed(0) + 'k';
                                        return value;
                                    }
                                }
                            },
                            x: { grid: { display: false } }
                        }
                    }
                });
            }

            // 3. LINE CHART (PEAK HOURS)
            const peakCanvas = document.getElementById('peakHoursChart');
            if (peakCanvas) {
                let chartStatus = Chart.getChart(peakCanvas);
                if (chartStatus != undefined) chartStatus.destroy();

                new Chart(peakCanvas, {
                    type: 'line',
                    data: {
                        labels: data.peakLabels || [],
                        datasets: [{
                            label: 'Tần suất (%)',
                            data: data.peakValues || [],
                            borderColor: '#2ecc71',
                            backgroundColor: 'rgba(46, 204, 113, 0.15)',
                            borderWidth: 2,
                            pointRadius: 3,
                            pointBackgroundColor: '#fff',
                            pointBorderColor: '#2ecc71',
                            fill: true,
                            tension: 0.4
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        plugins: { legend: { display: false } },
                        scales: {
                            y: {
                                beginAtZero: true,
                                max: 100,
                                ticks: { stepSize: 20 }
                            },
                            x: { grid: { display: false } }
                        }
                    }
                });
            }

            // 4. AI SUGGESTIONS
            const aiBox = document.getElementById('ai-suggestions-content');
            if (aiBox && data.aiSuggestions && data.aiSuggestions.length > 0) {
                let html = '<ul style="padding-left: 20px; margin: 0;">';
                data.aiSuggestions.forEach(msg => {
                    html += `<li style="margin-bottom: 8px; color: #444; font-size: 14px;">${msg}</li>`;
                });
                html += '</ul>';
                aiBox.innerHTML = html;
            }

        } catch (error) {
            console.error('Lỗi tải báo cáo:', error);
        }
    }
});