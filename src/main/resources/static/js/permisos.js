/**
 * permisos.js -- Chicken Biker
 *
 * Pestaña "Permisos por rol": marca visualmente qué permisos se ASIGNARÁN o
 * REVOCARÁN antes de guardar, habilita Guardar/Descartar solo si hay cambios
 * y avisa si se intenta salir con cambios sin guardar. El servidor valida y
 * aplica todo (GestionPermisosService); esto es solo presentación.
 */
(function () {
    'use strict';

    document.addEventListener('DOMContentLoaded', function () {
        var form = document.querySelector('[data-permisos-form]');
        if (!form) {
            return;
        }
        var interruptores = form.querySelectorAll('input[name="permisos"]');
        var resumen = form.querySelector('[data-resumen-cambios]');
        var botonGuardar = form.querySelector('[data-guardar]');
        var botonDescartar = form.querySelector('[data-descartar]');
        var enviando = false;

        function actualizar() {
            var asignar = 0;
            var revocar = 0;
            interruptores.forEach(function (input) {
                var original = input.getAttribute('data-original') === 'true';
                var fila = input.closest('.permiso-fila');
                var marca = fila ? fila.querySelector('[data-cambio]') : null;
                fila && fila.classList.remove('cambio-asignar', 'cambio-revocar');
                if (marca) {
                    marca.classList.add('d-none');
                    marca.textContent = '';
                }
                if (input.checked === original) {
                    return;
                }
                if (input.checked) {
                    asignar += 1;
                    fila && fila.classList.add('cambio-asignar');
                    if (marca) {
                        marca.textContent = '→ se asignará';
                        marca.className = 'permiso-cambio text-success';
                    }
                } else {
                    revocar += 1;
                    fila && fila.classList.add('cambio-revocar');
                    if (marca) {
                        marca.textContent = '→ se revocará';
                        marca.className = 'permiso-cambio text-danger';
                    }
                }
            });

            var hayCambios = asignar + revocar > 0;
            if (botonGuardar) {
                botonGuardar.disabled = !hayCambios;
            }
            if (botonDescartar) {
                botonDescartar.disabled = !hayCambios;
            }
            if (resumen) {
                resumen.textContent = hayCambios
                    ? 'Cambios pendientes: ' + asignar + ' por asignar, ' + revocar + ' por revocar.'
                    : 'Sin cambios pendientes.';
            }
            return hayCambios;
        }

        interruptores.forEach(function (input) {
            input.addEventListener('change', actualizar);
        });

        if (botonDescartar) {
            botonDescartar.addEventListener('click', function () {
                interruptores.forEach(function (input) {
                    input.checked = input.getAttribute('data-original') === 'true';
                });
                actualizar();
            });
        }

        form.addEventListener('submit', function () {
            enviando = true;
        });

        window.addEventListener('beforeunload', function (evento) {
            if (!enviando && actualizar()) {
                evento.preventDefault();
                evento.returnValue = '';
            }
        });

        actualizar();
    });
})();
