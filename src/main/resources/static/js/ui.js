/**
 * ui.js -- Chicken Biker
 *
 * Sistema CENTRALIZADO de diálogos y estados de carga. Expone una sola API,
 * window.ChickenBikerUI, para que ninguna vista use alert()/confirm()/prompt()
 * nativos ni cree su propio modal o pantalla de carga.
 *
 *   ChickenBikerUI.confirmar({ titulo, mensaje, tipo, textoConfirmar, textoCancelar })
 *       -> Promise<boolean>   (true solo si el usuario pulsa Confirmar)
 *   ChickenBikerUI.exito({ titulo, mensaje })        -> Promise<void>
 *   ChickenBikerUI.advertencia({ titulo, mensaje })  -> Promise<void>
 *   ChickenBikerUI.error({ titulo, mensaje })        -> Promise<void>
 *   ChickenBikerUI.mostrarCarga(texto?, { retraso }) / ocultarCarga()
 *   ChickenBikerUI.botonCargando(boton, texto?) / restaurarBoton(boton)
 *
 * Los diálogos reutilizan el componente Modal de Bootstrap 5.3 (ya cargado en
 * todas las vistas): aporta role="dialog", aria-modal, foco atrapado dentro
 * del modal, tecla Escape y fondo. Aquí se añade lo que Bootstrap no hace:
 * devolver el foco al elemento que abrió el modal, volver inerte el fondo,
 * bloquear el doble clic en Confirmar y no cerrar por clic fuera en las
 * operaciones críticas. Estilos propios en css/ui.css (no se toca style.css).
 *
 * Es solo interfaz: las reglas de negocio y la autorización las sigue
 * decidiendo el servidor.
 */
(function () {
    'use strict';

    var TIPOS = {
        peligro:     { icono: 'bi-exclamation-octagon-fill', clase: 'cb-dialogo-peligro',     boton: 'btn-cb-danger' },
        advertencia: { icono: 'bi-exclamation-triangle-fill', clase: 'cb-dialogo-advertencia', boton: 'btn-cb-primary' },
        info:        { icono: 'bi-question-circle-fill',      clase: 'cb-dialogo-info',        boton: 'btn-cb-primary' },
        exito:       { icono: 'bi-check-circle-fill',         clase: 'cb-dialogo-exito',       boton: 'btn-cb-primary' },
        error:       { icono: 'bi-x-octagon-fill',            clase: 'cb-dialogo-error',       boton: 'btn-cb-primary' }
    };

    var contador = 0;
    var dialogoAbierto = false;
    var colaDialogos = Promise.resolve();

    function modalDisponible() {
        return !!(window.bootstrap && window.bootstrap.Modal);
    }

    // Vuelve inerte (no enfocable ni clicable) todo lo que está detrás del modal.
    function fondoInerte(activar, excepto) {
        Array.prototype.forEach.call(document.body.children, function (el) {
            if (el === excepto || el.classList.contains('modal-backdrop') || el.id === 'cbCarga') {
                return;
            }
            if (activar) {
                if (!el.hasAttribute('inert')) {
                    el.setAttribute('inert', '');
                    el.setAttribute('data-cb-inerte', '');
                }
            } else if (el.hasAttribute('data-cb-inerte')) {
                el.removeAttribute('inert');
                el.removeAttribute('data-cb-inerte');
            }
        });
    }

    function crearBoton(texto, clases) {
        var boton = document.createElement('button');
        boton.type = 'button';
        boton.className = 'btn-cb ' + clases;
        boton.textContent = texto;
        return boton;
    }

    /**
     * Construye y muestra un diálogo. Resuelve con true (Confirmar) o false
     * (Cancelar, Escape, clic fuera cuando está permitido).
     * Los diálogos se encolan: nunca hay dos abiertos a la vez.
     */
    function abrirDialogo(opciones) {
        var siguiente = colaDialogos.then(function () {
            return new Promise(function (resolver) {
                mostrarDialogo(opciones, resolver);
            });
        });
        colaDialogos = siguiente.catch(function () {});
        return siguiente;
    }

    function mostrarDialogo(op, resolver) {
        var tipo = TIPOS[op.tipo] || TIPOS.info;
        var esConfirmacion = !!op.confirmacion;
        var critico = esConfirmacion && op.tipo === 'peligro';
        var origen = document.activeElement;

        // Sin Bootstrap (no debería ocurrir en vistas con common.js): degradar
        // al diálogo nativo antes que perder la confirmación o el mensaje.
        if (!modalDisponible()) {
            var texto = (op.titulo ? op.titulo + '\n\n' : '') + (op.mensaje || '');
            resolver(esConfirmacion ? window.confirm(texto) : (window.alert(texto), true));
            return;
        }

        ocultarCarga();
        contador += 1;
        var idTitulo = 'cbDialogoTitulo' + contador;
        var idMensaje = 'cbDialogoMensaje' + contador;

        var modal = document.createElement('div');
        modal.className = 'modal fade cb-dialogo ' + tipo.clase;
        modal.tabIndex = -1;
        modal.setAttribute('role', esConfirmacion ? 'alertdialog' : 'dialog');
        modal.setAttribute('aria-modal', 'true');
        modal.setAttribute('aria-labelledby', idTitulo);
        modal.setAttribute('aria-describedby', idMensaje);

        var caja = document.createElement('div');
        caja.className = 'modal-dialog modal-dialog-centered cb-dialogo-caja';
        var contenido = document.createElement('div');
        contenido.className = 'modal-content';

        var cuerpo = document.createElement('div');
        cuerpo.className = 'modal-body cb-dialogo-cuerpo';
        var icono = document.createElement('div');
        icono.className = 'cb-dialogo-icono';
        icono.setAttribute('aria-hidden', 'true');
        icono.innerHTML = '<i class="bi ' + tipo.icono + '"></i>';
        var titulo = document.createElement('h2');
        titulo.className = 'cb-dialogo-titulo';
        titulo.id = idTitulo;
        titulo.textContent = op.titulo || '';
        var mensaje = document.createElement('p');
        mensaje.className = 'cb-dialogo-mensaje';
        mensaje.id = idMensaje;
        mensaje.textContent = op.mensaje || '';
        cuerpo.appendChild(icono);
        cuerpo.appendChild(titulo);
        cuerpo.appendChild(mensaje);

        var pie = document.createElement('div');
        pie.className = 'modal-footer cb-dialogo-pie';
        var botonCancelar = null;
        var botonPrincipal;
        if (esConfirmacion) {
            botonCancelar = crearBoton(op.textoCancelar || 'Cancelar', 'btn-cb-secondary');
            botonPrincipal = crearBoton(op.textoConfirmar || 'Confirmar', tipo.boton);
            pie.appendChild(botonCancelar);
        } else {
            botonPrincipal = crearBoton(op.textoAceptar || 'Entendido', tipo.boton);
        }
        pie.appendChild(botonPrincipal);

        contenido.appendChild(cuerpo);
        contenido.appendChild(pie);
        caja.appendChild(contenido);
        modal.appendChild(caja);
        document.body.appendChild(modal);

        var resultado = false;
        var instancia = new window.bootstrap.Modal(modal, {
            // Operaciones críticas: un clic fuera NO cierra (evita errores
            // por accidente); Escape y Cancelar siempre cancelan.
            backdrop: critico ? 'static' : true,
            keyboard: true,
            focus: true
        });

        botonPrincipal.addEventListener('click', function () {
            if (botonPrincipal.disabled) {
                return;
            }
            botonPrincipal.disabled = true; // impide la doble confirmación
            if (botonCancelar) {
                botonCancelar.disabled = true;
            }
            resultado = true;
            instancia.hide();
        });
        if (botonCancelar) {
            botonCancelar.addEventListener('click', function () {
                instancia.hide();
            });
        }

        modal.addEventListener('shown.bs.modal', function () {
            // Bootstrap fija role="dialog" al mostrar; las confirmaciones
            // conservan role="alertdialog".
            modal.setAttribute('role', esConfirmacion ? 'alertdialog' : 'dialog');
            // En lo crítico el foco inicial va a Cancelar: un Enter accidental
            // no ejecuta la operación.
            (critico && botonCancelar ? botonCancelar : botonPrincipal).focus();
        });
        modal.addEventListener('hidden.bs.modal', function () {
            fondoInerte(false);
            dialogoAbierto = false;
            modal.remove();
            // dispose() se aplaza: si el usuario hizo clic fuera de un modal
            // crítico ("rebote" de backdrop static) y cerró enseguida,
            // Bootstrap aún tiene un callback pendiente sobre el elemento.
            window.setTimeout(function () { instancia.dispose(); }, 1000);
            if (origen && typeof origen.focus === 'function' && document.contains(origen)) {
                origen.focus();
            }
            resolver(resultado);
        });

        dialogoAbierto = true;
        fondoInerte(true, modal);
        instancia.show();
    }

    function confirmar(op) {
        op = op || {};
        return abrirDialogo({
            confirmacion: true,
            tipo: op.tipo || 'peligro',
            titulo: op.titulo || 'Confirmar acción',
            mensaje: op.mensaje || '¿Deseas continuar?',
            textoConfirmar: op.textoConfirmar,
            textoCancelar: op.textoCancelar
        });
    }

    function aviso(tipo, tituloPorDefecto) {
        return function (op) {
            op = typeof op === 'string' ? { mensaje: op } : (op || {});
            return abrirDialogo({
                tipo: tipo,
                titulo: op.titulo || tituloPorDefecto,
                mensaje: op.mensaje || '',
                textoAceptar: op.textoAceptar
            }).then(function () {});
        };
    }

    // ------------------------------------------------------------------
    // Pantalla de carga global
    // ------------------------------------------------------------------
    var RETRASO_CARGA_MS = 350;      // no aparece si la respuesta es rápida
    var MAXIMO_CARGA_MS = 20000;     // nunca queda bloqueada indefinidamente
    var temporizadorMostrar = null;
    var temporizadorMaximo = null;
    var elementoCarga = null;

    function obtenerCarga() {
        if (elementoCarga) {
            return elementoCarga;
        }
        elementoCarga = document.createElement('div');
        elementoCarga.id = 'cbCarga';
        elementoCarga.className = 'cb-carga';
        elementoCarga.setAttribute('role', 'status');
        elementoCarga.setAttribute('aria-live', 'polite');
        elementoCarga.setAttribute('aria-hidden', 'true');
        elementoCarga.innerHTML =
            '<div class="cb-carga-caja">' +
            '<span class="cb-carga-spinner" aria-hidden="true"></span>' +
            '<span class="cb-carga-texto"></span>' +
            '</div>';
        document.body.appendChild(elementoCarga);
        return elementoCarga;
    }

    function limpiarTemporizadores() {
        window.clearTimeout(temporizadorMostrar);
        window.clearTimeout(temporizadorMaximo);
        temporizadorMostrar = null;
        temporizadorMaximo = null;
    }

    /**
     * Muestra el indicador global. Con retraso (por defecto 350 ms) solo
     * aparece si la espera es real. Llamadas repetidas no lo duplican.
     */
    function mostrarCarga(texto, opciones) {
        var retraso = opciones && typeof opciones.retraso === 'number' ? opciones.retraso : RETRASO_CARGA_MS;
        if (dialogoAbierto) {
            return; // nunca encima de un modal de confirmación
        }
        limpiarTemporizadores();
        var el = obtenerCarga();
        el.querySelector('.cb-carga-texto').textContent = texto || 'Cargando…';
        temporizadorMostrar = window.setTimeout(function () {
            el.classList.add('is-visible');
            el.setAttribute('aria-hidden', 'false');
            document.body.setAttribute('aria-busy', 'true');
        }, retraso);
        temporizadorMaximo = window.setTimeout(ocultarCarga, retraso + MAXIMO_CARGA_MS);
    }

    function ocultarCarga() {
        limpiarTemporizadores();
        if (!elementoCarga) {
            return;
        }
        elementoCarga.classList.remove('is-visible');
        elementoCarga.setAttribute('aria-hidden', 'true');
        document.body.removeAttribute('aria-busy');
    }

    // ------------------------------------------------------------------
    // Spinner dentro de un botón (operaciones puntuales)
    // ------------------------------------------------------------------
    function botonCargando(boton, texto) {
        if (!boton || boton.getAttribute('data-cb-cargando') === 'true') {
            return;
        }
        boton.setAttribute('data-cb-cargando', 'true');
        boton.setAttribute('data-cb-html', boton.innerHTML);
        boton.setAttribute('aria-busy', 'true');
        boton.disabled = true;
        boton.classList.add('is-loading');
        var spinner = '<span class="cb-boton-spinner" aria-hidden="true"></span>';
        if (texto) {
            boton.innerHTML = spinner;
            boton.appendChild(document.createTextNode(' ' + texto));
        } else {
            boton.insertAdjacentHTML('afterbegin', spinner);
        }
    }

    function restaurarBoton(boton) {
        if (!boton || boton.getAttribute('data-cb-cargando') !== 'true') {
            return;
        }
        boton.innerHTML = boton.getAttribute('data-cb-html');
        boton.removeAttribute('data-cb-html');
        boton.removeAttribute('data-cb-cargando');
        boton.removeAttribute('aria-busy');
        boton.disabled = false;
        boton.classList.remove('is-loading');
    }

    // ------------------------------------------------------------------
    // Navegación: carga al salir de la página por un enlace interno
    // ------------------------------------------------------------------
    function esNavegacionInterna(evento, enlace) {
        if (evento.defaultPrevented || evento.button !== 0 ||
            evento.metaKey || evento.ctrlKey || evento.shiftKey || evento.altKey) {
            return false;
        }
        if (enlace.hasAttribute('download') || enlace.hasAttribute('data-sin-carga') ||
            enlace.hasAttribute('data-bs-toggle') || enlace.hasAttribute('data-confirm')) {
            return false;
        }
        var destino = enlace.getAttribute('target');
        if (destino && destino !== '_self') {
            return false;
        }
        var href = enlace.getAttribute('href');
        if (!href || href.charAt(0) === '#' || /^(javascript|mailto|tel):/i.test(href)) {
            return false;
        }
        var url = new URL(enlace.href, window.location.href);
        if (url.origin !== window.location.origin) {
            return false;
        }
        // Mismo documento con otro ancla: no hay navegación real
        return !(url.pathname === window.location.pathname && url.search === window.location.search && url.hash);
    }

    function initNavegacion() {
        document.addEventListener('click', function (evento) {
            var enlace = evento.target.closest && evento.target.closest('a[href]');
            if (!enlace) {
                return;
            }
            // Se decide al final del ciclo: si otro manejador canceló el clic
            // (p. ej. una confirmación), no se muestra la carga.
            window.setTimeout(function () {
                if (esNavegacionInterna(evento, enlace)) {
                    mostrarCarga('Cargando…');
                }
            }, 0);
        });

        // Envíos de formulario que llegan al servidor (POST/GET tradicionales).
        // Se evalúa después de los demás manejadores: si alguna validación
        // local canceló el envío, no aparece la carga.
        document.addEventListener('submit', function (evento) {
            var form = evento.target;
            if (!(form instanceof HTMLFormElement) || form.hasAttribute('data-sin-carga')) {
                return;
            }
            window.setTimeout(function () {
                if (!evento.defaultPrevented) {
                    mostrarCarga(form.getAttribute('data-texto-carga') || 'Procesando…');
                }
            }, 0);
        });

        // Volver con "Atrás" (página restaurada desde caché): sin carga.
        window.addEventListener('pageshow', ocultarCarga);
    }

    window.ChickenBikerUI = {
        confirmar: confirmar,
        exito: aviso('exito', 'Operación completada'),
        advertencia: aviso('advertencia', 'Atención'),
        error: aviso('error', 'No fue posible completar la operación'),
        mostrarCarga: mostrarCarga,
        ocultarCarga: ocultarCarga,
        botonCargando: botonCargando,
        restaurarBoton: restaurarBoton
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initNavegacion);
    } else {
        initNavegacion();
    }
})();
