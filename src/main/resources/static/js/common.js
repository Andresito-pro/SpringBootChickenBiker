
(function () {
    'use strict';

    function initHamburgerMenu() {
        var hamburgerBtn = document.getElementById('hamburgerBtn');
        var sidebar = document.getElementById('sidebar');
        var overlay = document.getElementById('sidebarOverlay');

        if (!hamburgerBtn || !sidebar) {
            return;
        }

        function abrirSidebar() {
            sidebar.classList.add('active');
            if (overlay) {
                overlay.classList.add('active');
            }
        }

        function cerrarSidebar() {
            sidebar.classList.remove('active');
            if (overlay) {
                overlay.classList.remove('active');
            }
        }

        hamburgerBtn.addEventListener('click', function () {
            if (sidebar.classList.contains('active')) {
                cerrarSidebar();
            } else {
                abrirSidebar();
            }
        });

        // CORRECCIÓN: #sidebarOverlay ya existía en el HTML de cada vista
        // pero no tenía ningún listener; tocar fuera del menú en móvil no
        // hacía nada. Ahora cierra el sidebar, como se espera.
        if (overlay) {
            overlay.addEventListener('click', cerrarSidebar);
        }

        // Al tocar un enlace del menú en móvil, se cierra el sidebar en vez
        // de dejarlo abierto tapando el contenido de la página de destino.
        sidebar.querySelectorAll('.nav-item').forEach(function (enlace) {
            enlace.addEventListener('click', cerrarSidebar);
        });
    }

    function initCurrentDateDisplay() {
        var currentDateElement = document.getElementById('currentDate');
        if (!currentDateElement) {
            return;
        }
        var fecha = new Date();
        var opciones = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' };
        var fechaTexto = fecha.toLocaleDateString('es-CO', opciones);
        currentDateElement.textContent = fechaTexto.charAt(0).toUpperCase() + fechaTexto.slice(1);
    }

    // FASE 9: antes cada botón de eliminar/inactivar tenía su propio
    // onclick="return confirm('mensaje literal')" repetido en 6 vistas
    // distintas (categorias.jsp, estadosMesas.jsp, mesas.jsp, productos.jsp,
    // roles.jsp, usuarios.jsp). Se reemplaza por un solo listener delegado
    // que busca el atributo data-confirm en el elemento donde se hizo clic
    // (o en su ancestro más cercano), preservando el mensaje personalizado
    // de cada botón sin duplicar la lógica de confirmación en cada JSP.
    //
    // Ahora la confirmación usa el modal de Chicken Biker (ui.js) en lugar del
    // window.confirm() nativo. Atributos opcionales del elemento:
    //   data-confirm-titulo   título del modal
    //   data-confirm-aceptar  texto del botón principal
    //   data-confirm-tipo     peligro (por defecto) | advertencia | info
    // El envío solo ocurre si el usuario pulsa Confirmar; al cancelar no se
    // envía nada al servidor.
    function initConfirmButtons() {
        document.addEventListener('click', function (evento) {
            var elemento = evento.target.closest('[data-confirm]');
            if (!elemento) {
                return;
            }
            // Segundo paso, ya confirmado: dejar seguir el envío normal.
            if (elemento.getAttribute('data-confirmado') === 'true') {
                elemento.removeAttribute('data-confirmado');
                return;
            }

            var form = elemento.form || null;
            // Si el formulario tiene campos inválidos, primero se muestran
            // las validaciones del navegador; la confirmación viene después.
            if (form && !form.noValidate && !form.checkValidity()) {
                return;
            }

            evento.preventDefault();
            evento.stopPropagation();
            if (elemento.getAttribute('data-confirmando') === 'true') {
                return; // modal ya abierto: ignora el doble clic
            }
            elemento.setAttribute('data-confirmando', 'true');

            window.ChickenBikerUI.confirmar({
                titulo: elemento.getAttribute('data-confirm-titulo') || 'Confirmar acción',
                mensaje: elemento.getAttribute('data-confirm'),
                textoConfirmar: elemento.getAttribute('data-confirm-aceptar') || 'Confirmar',
                tipo: elemento.getAttribute('data-confirm-tipo') || 'peligro'
            }).then(function (aceptado) {
                elemento.removeAttribute('data-confirmando');
                if (!aceptado) {
                    return;
                }
                if (form) {
                    // requestSubmit conserva el botón pulsado y dispara las
                    // validaciones y el bloqueo de doble envío normales.
                    elemento.setAttribute('data-confirmado', 'true');
                    if (typeof form.requestSubmit === 'function') {
                        form.requestSubmit(elemento.type === 'submit' ? elemento : undefined);
                    } else {
                        elemento.click();
                    }
                    elemento.removeAttribute('data-confirmado');
                } else if (elemento.tagName === 'A' && elemento.href) {
                    window.ChickenBikerUI.mostrarCarga('Cargando…');
                    window.location.assign(elemento.href);
                }
            });
        });
    }

    // Botón "ojo" del login (#togglePassword): alterna entre ocultar y mostrar
    // la contraseña. Solo cambia el type del input en el navegador.
    function initPasswordToggle() {
        var boton = document.getElementById('togglePassword');
        var campo = document.getElementById('user-pass');
        if (!boton || !campo) {
            return;
        }
        boton.addEventListener('click', function () {
            var visible = campo.type === 'text';
            campo.type = visible ? 'password' : 'text';
            var icono = boton.querySelector('i');
            if (icono) {
                icono.className = visible ? 'bi bi-eye' : 'bi bi-eye-slash';
            }
        });
    }

    // Impresión: cualquier elemento con data-action="print" llama a
    // window.print(). Si la URL llega con #imprimir (enlace "Imprimir" del
    // historial de facturas), la vista de detalle abre el diálogo sola.
    function initPrintButtons() {
        document.addEventListener('click', function (evento) {
            var elemento = evento.target.closest('[data-action="print"]');
            if (!elemento) {
                return;
            }
            evento.preventDefault();
            window.print();
        });

        if (window.location.hash === '#imprimir' && document.querySelector('[data-action="print"]')) {
            window.addEventListener('load', function () {
                // Se limpia el hash para que recargar la página no vuelva a imprimir.
                if (window.history && window.history.replaceState) {
                    window.history.replaceState(null, '', window.location.pathname + window.location.search);
                }
                window.print();
            });
        }
    }

    // Campos que el navegador considera "llenos" aunque solo tengan espacios.
    var SELECTOR_TEXTO_OBLIGATORIO =
        'input[required]:not([type]), input[required][type="text"], input[required][type="email"], ' +
        'input[required][type="tel"], input[required][type="password"], textarea[required]';

    // Formularios: el atributo "required" de HTML acepta "   " como válido y
    // el Servlet lo rechaza después ("Todos los campos obligatorios...").
    // Aquí se avisa antes de enviar. Además, una vez enviado, se bloquea un
    // segundo envío mientras carga la respuesta (evita doble cobro / doble
    // registro por doble clic). El servidor sigue validando todo.
    function initFormGuards() {
        document.addEventListener('input', function (evento) {
            var campo = evento.target;
            if (campo && campo.matches && campo.matches(SELECTOR_TEXTO_OBLIGATORIO)) {
                campo.setCustomValidity('');
                campo.classList.remove('is-invalid');
            }
        });

        // Se escucha en document (fase de burbuja) para ejecutarse DESPUÉS de
        // los manejadores propios de cada formulario (p. ej. tomarPedido.js):
        // si alguno ya canceló el envío, aquí no se hace nada.
        document.addEventListener('submit', function (evento) {
            var form = evento.target;
            if (!(form instanceof HTMLFormElement) || evento.defaultPrevented) {
                return;
            }

            if (form.getAttribute('data-submitting') === 'true') {
                evento.preventDefault();
                return;
            }

            if (!form.noValidate) {
                var invalido = null;
                form.querySelectorAll(SELECTOR_TEXTO_OBLIGATORIO).forEach(function (campo) {
                    if (!campo.disabled && campo.value.trim() === '') {
                        campo.setCustomValidity('Este campo no puede estar vacío ni contener solo espacios.');
                        campo.classList.add('is-invalid');
                        invalido = invalido || campo;
                    }
                });
                if (invalido) {
                    evento.preventDefault();
                    invalido.reportValidity();
                    invalido.focus();
                    return;
                }
            }

            form.setAttribute('data-submitting', 'true');
            // form.elements incluye también los botones externos asociados
            // con el atributo form="..." (cobrarPedido, dashboardCajero).
            var botones = Array.prototype.filter.call(form.elements, function (el) {
                return el.type === 'submit';
            });
            // Se deshabilitan en el siguiente ciclo para no interferir con el
            // envío que ya está en curso. El botón pulsado muestra además un
            // spinner (ui.js); los demás solo quedan deshabilitados.
            var pulsado = evento.submitter || botones[0];
            window.setTimeout(function () {
                botones.forEach(function (boton) {
                    if (boton === pulsado && window.ChickenBikerUI) {
                        window.ChickenBikerUI.botonCargando(boton);
                    } else {
                        boton.disabled = true;
                        boton.classList.add('is-loading');
                    }
                });
            }, 0);
        });

        // Si el navegador restaura la página desde su caché al volver
        // ("Atrás"), los formularios deben quedar utilizables otra vez.
        window.addEventListener('pageshow', function (evento) {
            if (!evento.persisted) {
                return;
            }
            document.querySelectorAll('form[data-submitting="true"]').forEach(function (form) {
                form.removeAttribute('data-submitting');
                Array.prototype.forEach.call(form.elements, function (el) {
                    if (el.type !== 'submit') {
                        return;
                    }
                    if (el.getAttribute('data-cb-cargando') === 'true' && window.ChickenBikerUI) {
                        window.ChickenBikerUI.restaurarBoton(el);
                    } else if (el.classList.contains('is-loading')) {
                        el.disabled = false;
                        el.classList.remove('is-loading');
                    }
                });
            });
        });
    }

    // Modales de Bootstrap (categorías, movimiento de caja, cobro de pedido):
    // al abrir se enfoca el primer campo editable; al cerrar se descarta lo
    // escrito para que el siguiente uso empiece limpio. Los eventos
    // shown/hidden.bs.modal de Bootstrap burbujean hasta document.
    function initModalForms() {
        document.addEventListener('shown.bs.modal', function (evento) {
            var modal = evento.target;
            var campo = modal.querySelector('[autofocus]') ||
                modal.querySelector('.modal-body input:not([type="hidden"]):not([disabled]), .modal-body select:not([disabled]), .modal-body textarea:not([disabled])');
            if (campo) {
                campo.focus();
            }
        });

        document.addEventListener('hidden.bs.modal', function (evento) {
            evento.target.querySelectorAll('form').forEach(function (form) {
                if (form.getAttribute('data-submitting') === 'true') {
                    return;
                }
                form.reset();
                form.querySelectorAll('.is-invalid').forEach(function (campo) {
                    campo.classList.remove('is-invalid');
                    if (campo.setCustomValidity) {
                        campo.setCustomValidity('');
                    }
                });
            });
        });
    }

    document.addEventListener('DOMContentLoaded', function () {
        initPasswordToggle();
        initHamburgerMenu();
        initCurrentDateDisplay();
        initConfirmButtons();
        initPrintButtons();
        initFormGuards();
        initModalForms();
    });
})();
