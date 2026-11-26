/**
 * tomarPedido.js -- Chicken Biker (Mesero: tomar pedido)
 *
 * Arma el pedido en el navegador (mesa + productos + cantidades) y, al
 * confirmar, genera los campos ocultos que PedidoServlet (accion=guardar)
 * ya espera: mesaId, productoId[] y cantidad[].
 *
 * El servidor sigue siendo la autoridad: vuelve a validar la caja abierta,
 * que la mesa exista y esté disponible, y recalcula precios y total desde la
 * base de datos. Los precios de aquí solo sirven para mostrar el subtotal.
 *
 * TIPO DE PEDIDO (radios name="tipoPedido"):
 *   - "En mesa"     -> se muestra la selección de mesa y es obligatoria.
 *   - "Para llevar" -> se oculta la selección de mesa, se descarta cualquier
 *                      mesa elegida y el campo mesaId se deshabilita (no se
 *                      envía). PedidoServlet repite esta misma regla.
 *
 * Cambios respecto a la versión anterior (misma funcionalidad):
 *   - Todo vive dentro de una IIFE: no hay variables ni funciones globales.
 *   - Los botones +/−/quitar del carrito ya no usan onclick="..." inline;
 *     se manejan con un único listener delegado sobre #carritoContainer.
 *   - Cada acceso al DOM valida que el elemento exista (sin errores si la
 *     vista cambia o si no hay mesas/productos).
 *   - Se eliminó el console.log de depuración.
 *   - Los montos se muestran con separador de miles, igual que el
 *     fmt:formatNumber "#,##0.00" del JSP.
 */
(function () {
    'use strict';

    var formatoMoneda = new Intl.NumberFormat('en-US', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });

    function formatearMonto(valor) {
        return '$ ' + formatoMoneda.format(valor);
    }

    function escapeHtml(texto) {
        return String(texto)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    document.addEventListener('DOMContentLoaded', function () {

        var form = document.getElementById('pedidoForm');
        var inputMesa = document.getElementById('mesaId');
        var carritoContainer = document.getElementById('carritoContainer');
        var totalPedido = document.getElementById('totalPedido');
        var btnConfirmar = document.getElementById('btnConfirmar');
        var mesaSeleccionadaLabel = document.getElementById('mesaSeleccionadaLabel');
        var mesaSeleccionadaTexto = document.getElementById('mesaSeleccionadaTexto');
        var mesaSeleccionadaIcono = document.getElementById('mesaSeleccionadaIcono');
        var mesaSection = document.getElementById('mesaSection');
        var tipoPedidoAyuda = document.getElementById('tipoPedidoAyuda');
        var radioParaLlevar = document.getElementById('tipoPedidoLlevar');
        var radiosTipo = document.querySelectorAll('input[name="tipoPedido"]');

        // Sin formulario o sin carrito no hay nada que inicializar.
        if (!form || !carritoContainer) {
            return;
        }

        var mesaSeleccionada = null;
        var carrito = [];

        // "Para llevar" solo existe si la vista trae el selector; sin él, el
        // comportamiento es exactamente el anterior (pedido en mesa).
        function esParaLlevar() {
            return !!(radioParaLlevar && radioParaLlevar.checked);
        }

        // ======================================================
        // BOTÓN CONFIRMAR
        // En mesa: requiere mesa + productos. Para llevar: solo productos.
        // ======================================================
        function actualizarBotonConfirmar() {
            if (btnConfirmar) {
                var tieneDestino = esParaLlevar() || !!mesaSeleccionada;
                btnConfirmar.disabled = !(tieneDestino && carrito.length > 0);
            }
        }

        // ======================================================
        // RENDERIZAR CARRITO
        // ======================================================
        function renderCarrito() {
            if (carrito.length === 0) {
                carritoContainer.innerHTML =
                    '<p class="text-muted">Seleccione una mesa y agregue productos.</p>';
                if (totalPedido) {
                    totalPedido.textContent = formatearMonto(0);
                }
                actualizarBotonConfirmar();
                return;
            }

            var html = '<ul class="cart-list">';
            var total = 0;

            carrito.forEach(function (item, index) {
                var subtotal = item.precio * item.cantidad;
                total += subtotal;

                html +=
                    '<li class="cart-item">' +
                        '<div>' +
                            '<span class="cart-item-name">' + escapeHtml(item.nombre) + '</span>' +
                            '<span class="cart-item-price">' + formatearMonto(item.precio) + ' c/u</span>' +
                            '<div class="qty-control">' +
                                '<button type="button" class="btn-cb btn-cb-icon btn-cb-secondary"' +
                                    ' aria-label="Quitar una unidad" data-cart-action="restar" data-index="' + index + '">' +
                                    '<i class="bi bi-dash-lg"></i>' +
                                '</button>' +
                                '<span class="qty-value">' + item.cantidad + '</span>' +
                                '<button type="button" class="btn-cb btn-cb-icon btn-cb-secondary"' +
                                    ' aria-label="Agregar una unidad" data-cart-action="sumar" data-index="' + index + '">' +
                                    '<i class="bi bi-plus-lg"></i>' +
                                '</button>' +
                            '</div>' +
                        '</div>' +
                        '<div class="cart-item-side">' +
                            '<span class="badge-cb badge-cb-primary">' + formatearMonto(subtotal) + '</span>' +
                            '<button type="button" class="btn-cb btn-cb-icon btn-cb-danger"' +
                                ' aria-label="Quitar producto" data-cart-action="quitar" data-index="' + index + '">' +
                                '<i class="bi bi-trash3"></i>' +
                            '</button>' +
                        '</div>' +
                    '</li>';
            });

            html += '</ul>';
            carritoContainer.innerHTML = html;

            if (totalPedido) {
                totalPedido.textContent = formatearMonto(total);
            }
            actualizarBotonConfirmar();
        }

        // ======================================================
        // CANTIDADES (+ / − / quitar) — un solo listener delegado
        // ======================================================
        function cambiarCantidad(index, delta) {
            var item = carrito[index];
            if (!item) {
                return;
            }
            if (item.cantidad + delta <= 0) {
                carrito.splice(index, 1);
            } else {
                item.cantidad += delta;
            }
            renderCarrito();
        }

        carritoContainer.addEventListener('click', function (evento) {
            var boton = evento.target.closest('[data-cart-action]');
            if (!boton) {
                return;
            }
            var index = parseInt(boton.getAttribute('data-index'), 10);
            if (isNaN(index)) {
                return;
            }
            switch (boton.getAttribute('data-cart-action')) {
                case 'sumar':
                    cambiarCantidad(index, 1);
                    break;
                case 'restar':
                    cambiarCantidad(index, -1);
                    break;
                case 'quitar':
                    carrito.splice(index, 1);
                    renderCarrito();
                    break;
            }
        });

        // ======================================================
        // SELECCIONAR MESA
        // ======================================================
        var botonesMesa = document.querySelectorAll('.mesa-btn');

        function limpiarSeleccionMesa() {
            mesaSeleccionada = null;
            botonesMesa.forEach(function (otro) {
                otro.classList.remove('active', 'seleccionada');
                otro.setAttribute('aria-pressed', 'false');
            });
            if (inputMesa) {
                inputMesa.value = '';
            }
        }

        // ======================================================
        // TIPO DE PEDIDO: En mesa / Para llevar
        // ======================================================
        function aplicarTipoPedido() {
            var llevar = esParaLlevar();

            if (llevar) {
                // No se exige ni se envía ninguna mesa.
                limpiarSeleccionMesa();
            }
            if (inputMesa) {
                inputMesa.disabled = llevar;
            }
            if (mesaSection) {
                mesaSection.classList.toggle('d-none', llevar);
            }
            if (mesaSeleccionadaTexto) {
                mesaSeleccionadaTexto.textContent = llevar ? 'Tipo' : 'Mesa seleccionada';
            }
            if (mesaSeleccionadaIcono) {
                mesaSeleccionadaIcono.classList.toggle('bi-bag', llevar);
                mesaSeleccionadaIcono.classList.toggle('bi-table', !llevar);
            }
            if (mesaSeleccionadaLabel) {
                mesaSeleccionadaLabel.textContent = llevar ? 'Para llevar (sin mesa)' : 'Ninguna';
            }
            if (tipoPedidoAyuda) {
                tipoPedidoAyuda.innerHTML = llevar
                    ? 'Mesa: <strong>no requerida</strong>. El pedido no ocupará ninguna mesa.'
                    : 'Selecciona una mesa disponible para el pedido.';
            }
            actualizarBotonConfirmar();
        }

        radiosTipo.forEach(function (radio) {
            radio.addEventListener('change', aplicarTipoPedido);
        });

        botonesMesa.forEach(function (boton) {
            boton.addEventListener('click', function () {
                if (boton.disabled || esParaLlevar()) {
                    return;
                }

                botonesMesa.forEach(function (otro) {
                    otro.classList.remove('active', 'seleccionada');
                    otro.setAttribute('aria-pressed', 'false');
                });

                boton.classList.add('active', 'seleccionada');
                boton.setAttribute('aria-pressed', 'true');

                mesaSeleccionada = boton.getAttribute('data-id');
                if (inputMesa) {
                    inputMesa.value = mesaSeleccionada;
                }
                if (mesaSeleccionadaLabel) {
                    mesaSeleccionadaLabel.textContent = 'Mesa ' + boton.getAttribute('data-numero');
                }

                actualizarBotonConfirmar();
            });
        });

        // ======================================================
        // AGREGAR PRODUCTO
        // ======================================================
        document.querySelectorAll('.agregar-producto').forEach(function (boton) {
            boton.addEventListener('click', function () {
                var id = boton.getAttribute('data-id');
                var nombre = boton.getAttribute('data-nombre') || '';
                var precio = parseFloat(boton.getAttribute('data-precio'));

                if (!id || isNaN(precio)) {
                    return;
                }

                var existente = carrito.filter(function (p) { return p.id === id; })[0];
                if (existente) {
                    existente.cantidad++;
                } else {
                    carrito.push({ id: id, nombre: nombre, precio: precio, cantidad: 1 });
                }

                renderCarrito();
            });
        });

        // ======================================================
        // ENVIAR PEDIDO
        // ======================================================
        form.addEventListener('submit', function (evento) {
            // Validaciones locales: se avisan con el modal de advertencia de
            // ui.js (antes window.alert). El pedido no se envía.
            if (!esParaLlevar() && !mesaSeleccionada) {
                evento.preventDefault();
                window.ChickenBikerUI.advertencia({
                    titulo: 'Falta la mesa',
                    mensaje: 'Debe seleccionar una mesa o elegir "Para llevar".'
                });
                return;
            }
            if (carrito.length === 0) {
                evento.preventDefault();
                window.ChickenBikerUI.advertencia({
                    titulo: 'Pedido vacío',
                    mensaje: 'Debe agregar al menos un producto.'
                });
                return;
            }

            // Quitar campos generados en un intento anterior.
            form.querySelectorAll('input[name="productoId"], input[name="cantidad"]').forEach(function (el) {
                el.remove();
            });

            // Los nombres deben coincidir exactamente con PedidoServlet.
            carrito.forEach(function (item) {
                var inputId = document.createElement('input');
                inputId.type = 'hidden';
                inputId.name = 'productoId';
                inputId.value = item.id;
                form.appendChild(inputId);

                var inputCantidad = document.createElement('input');
                inputCantidad.type = 'hidden';
                inputCantidad.name = 'cantidad';
                inputCantidad.value = item.cantidad;
                form.appendChild(inputCantidad);
            });
            // El bloqueo del doble envío lo aplica common.js para todos los
            // formularios del sistema.
        });

        renderCarrito();
        // Estado inicial coherente con el radio marcado por el servidor
        // (p. ej. al volver al formulario tras un error de validación).
        aplicarTipoPedido();
    });
})();
