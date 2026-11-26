/**
 * topbar.js -- Chicken Biker
 *
 * Comportamiento ÚNICO del topbar (fragments/topbar.html), compartido por todas
 * las vistas internas:
 *
 *  1. Indicador de caja: consulta /api/caja/estado al restaurar la página desde
 *     la caché del navegador, al volver a la pestaña y cada 60 s mientras la
 *     pestaña está visible. Si el estado cambió respecto al que se mostró al
 *     cargar la página, actualiza el indicador y ofrece recargar la vista (los
 *     botones y avisos que dependen de la caja se calculan en el servidor).
 *
 *  2. Campana de notificaciones (solo si el servidor la renderizó: permiso
 *     "Recibir notificaciones"): contador de no leídas, lista de recientes,
 *     marcar una o todas como leídas. Consulta cada 30 s con la pestaña visible
 *     y se detiene si el servidor responde 401/403.
 *
 * Todo el texto recibido se inserta con textContent (sin HTML). La autorización
 * real la aplica el servidor; esto es solo presentación.
 */
(function () {
    'use strict';

    if (window.__cbTopbarIniciado) {
        return; // el fragmento incluye el script una sola vez, pero por si acaso
    }
    window.__cbTopbarIniciado = true;

    var INTERVALO_CAJA_MS = 60000;
    var INTERVALO_NOTIF_MS = 30000;

    function pedirJson(url, opciones) {
        opciones = opciones || {};
        opciones.credentials = 'same-origin';
        opciones.cache = 'no-store';
        opciones.headers = Object.assign({ 'Accept': 'application/json' }, opciones.headers || {});
        return fetch(url, opciones).then(function (respuesta) {
            if (!respuesta.ok) {
                var error = new Error('HTTP ' + respuesta.status);
                error.status = respuesta.status;
                throw error;
            }
            return respuesta.json();
        });
    }

    // ------------------------------------------------------------------
    // 1. Estado de la caja
    // ------------------------------------------------------------------
    function initEstadoCaja() {
        var boton = document.querySelector('[data-estado-caja]');
        if (!boton) {
            return;
        }
        var url = boton.getAttribute('data-url');
        var estadoInicial = boton.getAttribute('data-open') === 'true';
        var avisoMostrado = false;
        var temporizador = null;

        function pintar(abierta) {
            boton.setAttribute('data-open', abierta ? 'true' : 'false');
            boton.title = abierta ? 'La caja 1 tiene un turno abierto' : 'No hay un turno de caja abierto';
            var etiqueta = boton.querySelector('.cash-label');
            if (etiqueta) {
                etiqueta.textContent = abierta ? 'Caja Abierta' : 'Caja Cerrada';
            }
        }

        function mostrarAvisoCambio(abierta) {
            if (avisoMostrado) {
                return;
            }
            var contenido = document.querySelector('main.content');
            if (!contenido) {
                return;
            }
            avisoMostrado = true;
            var aviso = document.createElement('div');
            aviso.className = 'alert alert-cb alert-info cb-aviso-caja';
            aviso.setAttribute('role', 'status');
            var icono = document.createElement('i');
            icono.className = 'bi bi-arrow-repeat alert-cb-icon';
            icono.setAttribute('aria-hidden', 'true');
            var cuerpo = document.createElement('div');
            cuerpo.className = 'alert-cb-body';
            cuerpo.textContent = abierta
                ? 'La caja se abrió mientras tenías esta pantalla abierta. Actualiza para ver la información vigente.'
                : 'La caja se cerró mientras tenías esta pantalla abierta. Actualiza para ver la información vigente.';
            var accion = document.createElement('button');
            accion.type = 'button';
            accion.className = 'btn-cb btn-cb-secondary btn-cb-sm ms-auto';
            accion.textContent = 'Actualizar';
            accion.addEventListener('click', function () {
                window.location.reload();
            });
            aviso.appendChild(icono);
            aviso.appendChild(cuerpo);
            aviso.appendChild(accion);
            contenido.insertBefore(aviso, contenido.firstChild);
        }

        function consultar() {
            if (!url) {
                return;
            }
            pedirJson(url).then(function (datos) {
                var abierta = !!(datos && datos.abierta);
                pintar(abierta);
                if (abierta !== estadoInicial) {
                    mostrarAvisoCambio(abierta);
                }
            }).catch(function () {
                // Sin conexión o sesión expirada: se conserva el último estado mostrado.
            });
        }

        function programar() {
            window.clearInterval(temporizador);
            temporizador = window.setInterval(function () {
                if (!document.hidden) {
                    consultar();
                }
            }, INTERVALO_CAJA_MS);
        }

        // Página restaurada desde la caché ("Atrás"): el HTML puede ser viejo.
        window.addEventListener('pageshow', function (evento) {
            if (evento.persisted) {
                consultar();
            }
        });
        document.addEventListener('visibilitychange', function () {
            if (!document.hidden) {
                consultar();
            }
        });
        programar();
    }

    // ------------------------------------------------------------------
    // 2. Notificaciones
    // ------------------------------------------------------------------
    function initNotificaciones() {
        var raiz = document.querySelector('[data-notificaciones]');
        if (!raiz) {
            return;
        }
        var url = raiz.getAttribute('data-url');
        var urlPedido = raiz.getAttribute('data-url-pedido');
        var cabeceraCsrf = raiz.getAttribute('data-csrf-header');
        var tokenCsrf = raiz.getAttribute('data-csrf-token');

        var toggle = raiz.querySelector('[data-notif-toggle]');
        var panel = raiz.querySelector('[data-notif-panel]');
        var contador = raiz.querySelector('[data-notif-contador]');
        var lector = raiz.querySelector('[data-notif-lector]');
        var lista = raiz.querySelector('[data-notif-lista]');
        var vacio = raiz.querySelector('[data-notif-vacio]');
        var resumenTexto = raiz.querySelector('[data-notif-resumen]');
        var botonTodas = raiz.querySelector('[data-notif-todas]');
        var errorCaja = raiz.querySelector('[data-notif-error]');

        var temporizador = null;
        var detenido = false;
        var ultimoNoLeidas = null;

        function cabecerasPost() {
            var h = {};
            if (cabeceraCsrf && tokenCsrf) {
                h[cabeceraCsrf] = tokenCsrf;
            }
            return h;
        }

        function pintar(datos) {
            var noLeidas = datos && typeof datos.noLeidas === 'number' ? datos.noLeidas : 0;
            var recientes = (datos && datos.recientes) || [];

            contador.textContent = noLeidas > 99 ? '99+' : String(noLeidas);
            contador.classList.toggle('d-none', noLeidas === 0);
            if (ultimoNoLeidas !== null && noLeidas > ultimoNoLeidas) {
                toggle.classList.remove('notif-nueva');
                void toggle.offsetWidth; // reinicia la animación
                toggle.classList.add('notif-nueva');
            }
            ultimoNoLeidas = noLeidas;

            var texto = noLeidas === 0 ? 'Sin pendientes'
                : (noLeidas === 1 ? '1 pendiente de revisar' : noLeidas + ' pendientes de revisar');
            resumenTexto.textContent = texto;
            lector.textContent = noLeidas === 0 ? 'Sin notificaciones nuevas' : texto;
            toggle.setAttribute('aria-label', 'Notificaciones: ' + texto);
            botonTodas.disabled = noLeidas === 0;

            lista.textContent = '';
            recientes.forEach(function (n) {
                lista.appendChild(crearItem(n));
            });
            vacio.classList.toggle('d-none', recientes.length > 0);
            errorCaja.classList.add('d-none');
        }

        function crearItem(n) {
            var li = document.createElement('li');
            var boton = document.createElement('button');
            boton.type = 'button';
            boton.className = 'notif-item' + (n.leida ? '' : ' no-leida');

            var icono = document.createElement('span');
            icono.className = 'notif-item-icono';
            icono.setAttribute('aria-hidden', 'true');
            icono.innerHTML = '<i class="bi bi-receipt"></i>';

            var cuerpo = document.createElement('span');
            cuerpo.className = 'notif-item-cuerpo';
            var titulo = document.createElement('strong');
            titulo.textContent = n.titulo || 'Notificación';
            var mensaje = document.createElement('span');
            mensaje.className = 'notif-item-mensaje';
            mensaje.textContent = n.mensaje || '';
            var fecha = document.createElement('span');
            fecha.className = 'notif-item-fecha';
            fecha.textContent = n.fecha || '';
            cuerpo.appendChild(titulo);
            cuerpo.appendChild(mensaje);
            cuerpo.appendChild(fecha);

            boton.appendChild(icono);
            boton.appendChild(cuerpo);
            if (!n.leida) {
                var punto = document.createElement('span');
                punto.className = 'notif-item-punto';
                punto.setAttribute('aria-label', 'No leída');
                boton.appendChild(punto);
            }

            boton.addEventListener('click', function () {
                var destino = n.idPedido ? urlPedido + encodeURIComponent(n.idPedido) : null;
                var marcar = n.leida ? Promise.resolve(null)
                    : pedirJson(url + '/' + encodeURIComponent(n.id) + '/leida',
                        { method: 'POST', headers: cabecerasPost() });
                marcar.then(function (datos) {
                    if (datos) {
                        pintar(datos);
                    }
                }).catch(function () {}).then(function () {
                    if (destino) {
                        window.location.assign(destino);
                    }
                });
            });
            li.appendChild(boton);
            return li;
        }

        function consultar() {
            if (detenido) {
                return;
            }
            pedirJson(url).then(pintar).catch(function (error) {
                if (error.status === 401 || error.status === 403) {
                    detenido = true; // sin sesión o sin permiso: no se insiste
                    window.clearInterval(temporizador);
                }
                errorCaja.classList.remove('d-none');
            });
        }

        function abrir() {
            panel.hidden = false;
            toggle.setAttribute('aria-expanded', 'true');
            consultar();
        }

        function cerrar() {
            panel.hidden = true;
            toggle.setAttribute('aria-expanded', 'false');
        }

        toggle.addEventListener('click', function (evento) {
            evento.stopPropagation();
            if (panel.hidden) {
                abrir();
            } else {
                cerrar();
            }
        });
        document.addEventListener('click', function (evento) {
            if (!panel.hidden && !raiz.contains(evento.target)) {
                cerrar();
            }
        });
        document.addEventListener('keydown', function (evento) {
            if (evento.key === 'Escape' && !panel.hidden) {
                cerrar();
                toggle.focus();
            }
        });

        botonTodas.addEventListener('click', function () {
            botonTodas.disabled = true;
            pedirJson(url + '/leidas', { method: 'POST', headers: cabecerasPost() })
                .then(pintar)
                .catch(function () {
                    botonTodas.disabled = false;
                    errorCaja.classList.remove('d-none');
                });
        });

        document.addEventListener('visibilitychange', function () {
            if (!document.hidden) {
                consultar();
            }
        });
        window.addEventListener('pageshow', function (evento) {
            if (evento.persisted) {
                consultar();
            }
        });

        consultar();
        temporizador = window.setInterval(function () {
            if (!document.hidden) {
                consultar();
            }
        }, INTERVALO_NOTIF_MS);
    }

    function iniciar() {
        initEstadoCaja();
        initNotificaciones();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', iniciar);
    } else {
        iniciar();
    }
})();
