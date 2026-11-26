/**
 * validacionContrasena.js -- Chicken Biker
 *
 * Indicadores de contraseña segura en los formularios de usuarios
 * (usuarios/nuevoUsuario.html y usuarios/editarUsuario.html).
 *
 * Marcado esperado:
 *   <input type="password" data-politica-contrasena [data-contrasena-opcional]
 *          aria-describedby="requisitosContrasena ...">
 *   <ul id="requisitosContrasena"><li data-requisito="longitud">...</li>...</ul>
 *
 * data-contrasena-opcional: en la edición, dejar el campo VACÍO conserva la
 * contraseña actual; si se escribe algo (aunque sean espacios) debe cumplir.
 *
 * Es solo ayuda visual y bloqueo de envío en el navegador: las mismas reglas
 * se aplican en el servidor (PoliticaContrasena.java), que es quien decide.
 */
(function () {
    'use strict';

    var LONGITUD_MINIMA = 8;

    var REQUISITOS = {
        longitud: function (v) { return Array.from(v).length >= LONGITUD_MINIMA; },
        mayuscula: function (v) { return /\p{Lu}/u.test(v); },
        minuscula: function (v) { return /\p{Ll}/u.test(v); },
        numero: function (v) { return /\p{Nd}/u.test(v); },
        especial: function (v) { return /[^\p{L}\p{N}\s]/u.test(v); }
    };

    function evaluar(valor) {
        var resultado = {};
        Object.keys(REQUISITOS).forEach(function (clave) {
            resultado[clave] = REQUISITOS[clave](valor);
        });
        return resultado;
    }

    function initCampo(campo) {
        var lista = document.getElementById('requisitosContrasena');
        var feedback = document.getElementById('contrasenaFeedback');
        var opcional = campo.hasAttribute('data-contrasena-opcional');
        var form = campo.form;
        if (!lista || !form) {
            return;
        }

        function pintar() {
            var valor = campo.value;
            var estado = evaluar(valor);
            var todos = true;
            lista.querySelectorAll('[data-requisito]').forEach(function (item) {
                var ok = !!estado[item.getAttribute('data-requisito')];
                todos = todos && ok;
                item.classList.toggle('text-success', ok);
                item.classList.toggle('text-muted', !ok);
                var icono = item.querySelector('i');
                if (icono) {
                    icono.className = ok ? 'bi bi-check-circle-fill me-1' : 'bi bi-circle me-1';
                }
                var estadoSr = item.querySelector('.visually-hidden');
                if (estadoSr) {
                    estadoSr.textContent = ok ? ' (cumplido)' : ' (pendiente)';
                }
            });
            var soloEspacios = valor.length > 0 && valor.trim() === '';
            var vacio = valor.length === 0;
            var valido = (opcional && vacio) || (todos && !soloEspacios && !vacio);
            return { valido: valido, vacio: vacio, soloEspacios: soloEspacios };
        }

        function mensajeError(r) {
            if (r.vacio) {
                return 'La contraseña es obligatoria.';
            }
            if (r.soloEspacios) {
                return 'La contraseña no puede contener solo espacios.';
            }
            var faltan = [];
            lista.querySelectorAll('[data-requisito]').forEach(function (item) {
                if (!REQUISITOS[item.getAttribute('data-requisito')](campo.value)) {
                    faltan.push(item.getAttribute('data-texto'));
                }
            });
            return 'La contraseña no es segura. Falta: ' + faltan.join(', ') + '.';
        }

        function marcarInvalido(r) {
            var mensaje = mensajeError(r);
            campo.classList.add('is-invalid');
            campo.setAttribute('aria-invalid', 'true');
            campo.setCustomValidity(mensaje);
            if (feedback) {
                feedback.textContent = mensaje;
            }
        }

        function limpiar() {
            campo.classList.remove('is-invalid');
            campo.removeAttribute('aria-invalid');
            campo.setCustomValidity('');
            if (feedback) {
                feedback.textContent = '';
            }
        }

        campo.addEventListener('input', function () {
            var r = pintar();
            if (r.valido) {
                limpiar();
            } else if (campo.classList.contains('is-invalid')) {
                marcarInvalido(r); // actualiza el mensaje mientras corrige
            }
        });

        campo.addEventListener('blur', function () {
            var r = pintar();
            if (!r.valido && !(r.vacio && !campo.required)) {
                marcarInvalido(r);
            }
        });

        // Listener en el propio formulario: se ejecuta ANTES que el de
        // common.js (registrado en document), que no actúa si ya se canceló.
        form.addEventListener('submit', function (evento) {
            var r = pintar();
            if (!r.valido) {
                evento.preventDefault();
                marcarInvalido(r);
                campo.reportValidity();
                campo.focus();
            }
        });

        pintar();
    }

    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('input[data-politica-contrasena]').forEach(initCampo);
    });
})();
