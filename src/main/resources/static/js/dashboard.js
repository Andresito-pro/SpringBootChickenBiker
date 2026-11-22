/**
 * dashboard.js -- Chicken Biker
 *
 * Gráficas del dashboard del ADMINISTRADOR (dashboards/dashboard.html).
 *
 * Historial de ventas diarias: consulta GET /api/dashboard/ventas-diarias
 * (DashboardController) con los filtros de 7 / 30 / 90 días o un rango
 * personalizado y dibuja una gráfica de barras con Chart.js. Los datos son
 * los recibos realmente pagados en la base de datos; el servidor ya incluye
 * los días sin ventas con valor 0. Los montos se muestran en COP.
 *
 * El endpoint solo responde a usuarios con rol Administrador (SecurityConfig):
 * 401 = sesión vencida (se envía al login), 403 = sin permiso.
 */
(function () {
    'use strict';

    // Los colores de las gráficas salen de las variables de style.css
    // (:root), para que la paleta viva en un solo lugar.
    function cssVar(nombre) {
        return getComputedStyle(document.documentElement).getPropertyValue(nombre).trim();
    }

    var formatoCOP = new Intl.NumberFormat('es-CO', {
        style: 'currency', currency: 'COP', minimumFractionDigits: 0, maximumFractionDigits: 2
    });
    var formatoEntero = new Intl.NumberFormat('es-CO');

    function cop(valor) {
        return formatoCOP.format(Number(valor) || 0);
    }

    // "2026-11-22" -> Date local (sin desfase de zona horaria)
    function aFecha(iso) {
        var p = String(iso).split('-');
        return new Date(Number(p[0]), Number(p[1]) - 1, Number(p[2]));
    }

    function aIso(fecha) {
        var m = String(fecha.getMonth() + 1).padStart(2, '0');
        var d = String(fecha.getDate()).padStart(2, '0');
        return fecha.getFullYear() + '-' + m + '-' + d;
    }

    function etiquetaCorta(iso) {
        return aFecha(iso).toLocaleDateString('es-CO', { day: '2-digit', month: 'short' });
    }

    function etiquetaLarga(iso) {
        var t = aFecha(iso).toLocaleDateString('es-CO', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
        return t.charAt(0).toUpperCase() + t.slice(1);
    }

    function initVentasDiarias() {
        var tarjeta = document.getElementById('ventasDiarias');
        var canvas = document.getElementById('ventasDiariasChart');
        if (!tarjeta || !canvas) {
            return;
        }

        var endpoint = tarjeta.getAttribute('data-endpoint');
        var urlLogin = tarjeta.getAttribute('data-login') || '/login';
        var contenedor = document.getElementById('ventasGraficaContenedor');
        var vacio = document.getElementById('ventasVacio');
        var errorCaja = document.getElementById('ventasError');
        var errorTexto = document.getElementById('ventasErrorTexto');
        var formRango = document.getElementById('ventasRangoForm');
        var inputDesde = document.getElementById('ventasDesde');
        var inputHasta = document.getElementById('ventasHasta');
        var botones = tarjeta.querySelectorAll('[data-rango]');
        var tabla = document.getElementById('ventasTabla');

        var grafica = null;
        var peticion = null;

        if (typeof window.Chart === 'undefined') {
            mostrarError('No se pudo cargar la librería de gráficas. Revisa tu conexión a internet.');
            return;
        }

        // Fechas por defecto del rango personalizado: últimos 30 días, sin futuro
        var hoy = new Date();
        var hace30 = new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate() - 29);
        inputHasta.value = aIso(hoy);
        inputDesde.value = aIso(hace30);
        inputHasta.max = aIso(hoy);
        inputDesde.max = aIso(hoy);

        function mostrarError(mensaje) {
            errorTexto.textContent = mensaje;
            errorCaja.classList.remove('d-none');
        }

        function ocultarError() {
            errorCaja.classList.add('d-none');
            errorTexto.textContent = '';
        }

        function marcarActivo(boton) {
            botones.forEach(function (b) {
                var activo = b === boton;
                b.classList.toggle('active', activo);
                b.setAttribute('aria-pressed', activo ? 'true' : 'false');
            });
        }

        function cargar(parametros) {
            if (peticion) {
                peticion.abort();
            }
            peticion = new AbortController();
            ocultarError();
            contenedor.classList.add('is-cargando');

            var url = endpoint + '?' + new URLSearchParams(parametros).toString();
            fetch(url, {
                credentials: 'same-origin',
                headers: { 'Accept': 'application/json' },
                signal: peticion.signal
            })
                .then(function (respuesta) {
                    if (respuesta.status === 401) {
                        window.location.href = urlLogin;
                        return null;
                    }
                    if (respuesta.status === 403) {
                        throw new Error('No tienes permiso para consultar las ventas.');
                    }
                    return respuesta.json().catch(function () { return null; }).then(function (cuerpo) {
                        if (!respuesta.ok) {
                            throw new Error((cuerpo && cuerpo.error) || 'No fue posible consultar las ventas.');
                        }
                        if (!cuerpo || !Array.isArray(cuerpo.dias)) {
                            throw new Error('La respuesta del servidor no tiene el formato esperado.');
                        }
                        return cuerpo;
                    });
                })
                .then(function (datos) {
                    if (datos) {
                        pintar(datos);
                    }
                })
                .catch(function (error) {
                    if (error.name === 'AbortError') {
                        return;
                    }
                    mostrarError(error.message || 'No fue posible consultar las ventas.');
                })
                .finally(function () {
                    contenedor.classList.remove('is-cargando');
                });
        }

        function pintarResumen(datos) {
            var dias = datos.dias;
            var total = Number(datos.totalPeriodo) || 0;
            var mejor = null;
            dias.forEach(function (d) {
                if (Number(d.total) > 0 && (!mejor || Number(d.total) > Number(mejor.total))) {
                    mejor = d;
                }
            });
            document.getElementById('ventasTotal').textContent = cop(total);
            document.getElementById('ventasRecibos').textContent = formatoEntero.format(datos.cantidadRecibos || 0);
            document.getElementById('ventasPromedio').textContent = cop(dias.length ? total / dias.length : 0);
            document.getElementById('ventasMejorDia').textContent =
                mejor ? etiquetaCorta(mejor.fecha) + ' · ' + cop(mejor.total) : '—';
        }

        function pintarTabla(dias) {
            tabla.textContent = '';
            dias.slice().reverse().forEach(function (d) {
                var fila = document.createElement('tr');
                [etiquetaLarga(d.fecha), formatoEntero.format(d.cantidadRecibos || 0), cop(d.total)]
                    .forEach(function (valor, i) {
                        var celda = document.createElement('td');
                        celda.textContent = valor;
                        if (i > 0) {
                            celda.className = 'text-end';
                        }
                        fila.appendChild(celda);
                    });
                tabla.appendChild(fila);
            });
        }

        function pintar(datos) {
            var dias = datos.dias;
            pintarResumen(datos);
            pintarTabla(dias);

            var hayVentas = dias.some(function (d) { return Number(d.total) > 0; });
            vacio.classList.toggle('d-none', hayVentas);
            contenedor.classList.toggle('d-none', !hayVentas);
            if (!hayVentas) {
                return;
            }

            var etiquetas = dias.map(function (d) { return etiquetaCorta(d.fecha); });
            var valores = dias.map(function (d) { return Number(d.total) || 0; });

            if (grafica) {
                grafica.data.labels = etiquetas;
                grafica.data.datasets[0].data = valores;
                grafica.$dias = dias;
                grafica.update();
                return;
            }

            var tinta = cssVar('--slate-500');
            var rejilla = cssVar('--slate-200');
            grafica = new Chart(canvas, {
                type: 'bar',
                data: {
                    labels: etiquetas,
                    datasets: [{
                        label: 'Ventas pagadas',
                        data: valores,
                        backgroundColor: cssVar('--red'),
                        hoverBackgroundColor: cssVar('--red-dark'),
                        borderRadius: { topLeft: 4, topRight: 4 },
                        borderSkipped: 'bottom',
                        maxBarThickness: 36,
                        categoryPercentage: 0.9,
                        barPercentage: 0.9
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    interaction: { mode: 'index', intersect: false },
                    plugins: {
                        legend: { display: false },
                        tooltip: {
                            displayColors: false,
                            callbacks: {
                                title: function (items) {
                                    var d = grafica.$dias[items[0].dataIndex];
                                    return cop(d.total);
                                },
                                label: function (item) {
                                    var d = grafica.$dias[item.dataIndex];
                                    var n = Number(d.cantidadRecibos) || 0;
                                    return etiquetaLarga(d.fecha) + ' · ' + n + (n === 1 ? ' recibo' : ' recibos');
                                }
                            }
                        }
                    },
                    scales: {
                        x: {
                            grid: { display: false },
                            ticks: { color: tinta, autoSkip: true, maxRotation: 0, autoSkipPadding: 12 }
                        },
                        y: {
                            beginAtZero: true,
                            border: { display: false },
                            grid: { color: rejilla },
                            ticks: {
                                color: tinta,
                                maxTicksLimit: 6,
                                callback: function (valor) {
                                    return new Intl.NumberFormat('es-CO', {
                                        style: 'currency', currency: 'COP',
                                        notation: 'compact', maximumFractionDigits: 1
                                    }).format(valor);
                                }
                            }
                        }
                    }
                }
            });
            grafica.$dias = dias;
        }

        botones.forEach(function (boton) {
            boton.addEventListener('click', function () {
                var rango = boton.getAttribute('data-rango');
                marcarActivo(boton);
                if (rango === 'personalizado') {
                    formRango.classList.remove('d-none');
                    inputDesde.focus();
                    return;
                }
                formRango.classList.add('d-none');
                cargar({ dias: rango });
            });
        });

        formRango.addEventListener('submit', function (evento) {
            evento.preventDefault();
            var desde = inputDesde.value;
            var hasta = inputHasta.value;
            if (!desde || !hasta) {
                mostrarError('Selecciona la fecha inicial y la fecha final.');
                return;
            }
            if (desde > hasta) {
                mostrarError('La fecha final no puede ser anterior a la fecha inicial.');
                return;
            }
            cargar({ desde: desde, hasta: hasta });
        });

        cargar({ dias: 7 });
    }

    // Funciones heredadas de la versión JSP; se conservan por compatibilidad.
    function initSalesHourChart(canvasId, ventasPorHora) {
        var canvas = document.getElementById(canvasId);
        if (!canvas || typeof window.Chart === 'undefined') {
            return;
        }
        new Chart(canvas, {
            type: 'line',
            data: {
                labels: ventasPorHora.map(function (item) { return item.hora; }),
                datasets: [{
                    label: 'Ventas',
                    data: ventasPorHora.map(function (item) { return item.total; }),
                    borderColor: cssVar('--red'),
                    backgroundColor: cssVar('--cream-2'),
                    pointBackgroundColor: cssVar('--orange'),
                    borderWidth: 3,
                    tension: 0.35,
                    fill: true,
                    pointRadius: 4
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false } }
            }
        });
    }

    function initTablesDonutChart(canvasId, mesasOcupadas, totalMesas) {
        var canvas = document.getElementById(canvasId);
        if (!canvas || typeof window.Chart === 'undefined') {
            return;
        }
        var mesasDisponibles = Math.max(0, totalMesas - mesasOcupadas);
        new Chart(canvas, {
            type: 'doughnut',
            data: {
                labels: ['Ocupadas', 'Disponibles'],
                datasets: [{
                    data: [mesasOcupadas, mesasDisponibles],
                    backgroundColor: [cssVar('--red'), cssVar('--color-success-action')],
                    borderWidth: 0
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                cutout: '72%',
                plugins: { legend: { position: 'bottom' } }
            }
        });
    }

    window.ChickenBikerDashboard = {
        initVentasDiarias: initVentasDiarias,
        initSalesHourChart: initSalesHourChart,
        initTablesDonutChart: initTablesDonutChart
    };

    document.addEventListener('DOMContentLoaded', initVentasDiarias);
})();
