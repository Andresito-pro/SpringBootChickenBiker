/**
 * filtros.js -- Chicken Biker
 *
 * Motor ÚNICO y reutilizable de búsqueda y filtrado en el cliente, para
 * listados que el Servlet ya entregó completos en el HTML (tablas y
 * cuadrículas de tarjetas). Se configura sólo con atributos data-* en el JSP;
 * no contiene nombres de páginas, IDs ni datos de ninguna vista.
 *
 * Qué hace y qué NO hace:
 *   - Oculta/muestra elementos ya cargados. No llama al servidor, no modifica
 *     datos y no reemplaza ninguna validación ni autorización del backend.
 *   - Si un elemento no existe en la página (por rol, por estado vacío...),
 *     simplemente no se inicializa: nunca lanza errores por null.
 *
 * ------------------------------------------------------------------------
 * CONTRATO (atributos que se usan en el JSP)
 * ------------------------------------------------------------------------
 *   [data-filter-scope]             Contenedor que agrupa controles + elementos.
 *                                   Puede haber varios por página.
 *
 *   Elementos filtrables:
 *   [data-filter-item]              Cada fila / tarjeta.
 *     data-search="texto"           Texto en el que busca el buscador
 *                                   (si falta, se usa el textContent).
 *     data-<clave>="valor"          Valor para filtros por campo
 *                                   (ej. data-estado="activo", data-rol="2").
 *
 *   Controles:
 *   input[data-filter-search]       Buscador de texto libre.
 *   [data-filter-group="clave"]     Grupo de píldoras; cada botón lleva
 *     > [data-filter-value="v"]     data-filter-value ("" = Todos).
 *       [data-filter-counter]       (opcional) span donde se escribe el total.
 *       [data-filter-optional]      (opcional) la píldora se oculta si su
 *                                   total es 0.
 *   select[data-filter-field="clave"]      Filtro por select ("" = todos).
 *   input[type=date][data-filter-date="clave"]  Fecha exacta (yyyy-MM-dd).
 *   [data-filter-clear]             Botón "Limpiar": restablece todo.
 *   [data-filter-result]            Texto "Mostrando X de Y".
 *   [data-filter-empty]             Mensaje "sin coincidencias" (arranca con
 *                                   la clase d-none).
 *
 * Todos los criterios se combinan con Y lógico (búsqueda + píldoras +
 * selects + fecha). En el buscador cada palabra debe aparecer, en cualquier
 * orden, sin distinguir mayúsculas ni tildes.
 */
(function () {
    'use strict';

    var CLASE_OCULTO = 'd-none';
    var ESPERA_BUSQUEDA_MS = 120;

    // Minúsculas, sin tildes y con espacios colapsados, para que
    // "José" == "jose" y "  ana   maria " == "ana maria".
    function normalizar(texto) {
        return String(texto == null ? '' : texto)
            .normalize('NFD')
            .replace(/[\u0300-\u036f]/g, '')
            .toLowerCase()
            .replace(/\s+/g, ' ')
            .trim();
    }

    function aLista(nodos) {
        return Array.prototype.slice.call(nodos || []);
    }

    function initScope(scope) {
        var items = aLista(scope.querySelectorAll('[data-filter-item]'));
        var buscador = scope.querySelector('[data-filter-search]');
        var grupos = aLista(scope.querySelectorAll('[data-filter-group]'));
        var selects = aLista(scope.querySelectorAll('[data-filter-field]'));
        var fechas = aLista(scope.querySelectorAll('[data-filter-date]'));
        var botonesLimpiar = aLista(scope.querySelectorAll('[data-filter-clear]'));
        var mensajeVacio = scope.querySelector('[data-filter-empty]');
        var resultado = scope.querySelector('[data-filter-result]');

        // Texto normalizado de cada elemento, calculado una sola vez.
        var textoPorItem = new Map();
        items.forEach(function (item) {
            var fuente = item.hasAttribute('data-search')
                ? item.getAttribute('data-search')
                : item.textContent;
            textoPorItem.set(item, normalizar(fuente));
        });

        // ---------- Estado de cada grupo de píldoras ----------
        var estadoGrupos = grupos.map(function (grupo) {
            var botones = aLista(grupo.querySelectorAll('[data-filter-value]'));
            var porDefecto = botones.filter(function (b) {
                return b.getAttribute('data-filter-value') === '';
            })[0] || botones[0] || null;
            var activo = botones.filter(function (b) {
                return b.classList.contains('active');
            })[0] || porDefecto;

            return {
                clave: grupo.getAttribute('data-filter-group'),
                botones: botones,
                porDefecto: porDefecto,
                valor: activo ? activo.getAttribute('data-filter-value') : ''
            };
        });

        function valorDe(item, clave) {
            var valor = item.getAttribute('data-' + clave);
            return valor == null ? '' : String(valor);
        }

        // ---------- Contadores por píldora (sobre el total real) ----------
        estadoGrupos.forEach(function (g) {
            g.botones.forEach(function (boton) {
                var valor = boton.getAttribute('data-filter-value');
                var total = valor === ''
                    ? items.length
                    : items.filter(function (it) { return valorDe(it, g.clave) === valor; }).length;

                var contador = boton.querySelector('[data-filter-counter]');
                if (contador) {
                    contador.textContent = total;
                }
                if (boton.hasAttribute('data-filter-optional') && total === 0) {
                    boton.classList.add(CLASE_OCULTO);
                }
            });
        });

        function pintarPildoras(g) {
            g.botones.forEach(function (boton) {
                var activo = boton.getAttribute('data-filter-value') === g.valor;
                boton.classList.toggle('active', activo);
                boton.setAttribute('aria-pressed', activo ? 'true' : 'false');
            });
        }

        function hayFiltrosActivos(terminos) {
            if (terminos.length > 0) {
                return true;
            }
            var pildoraActiva = estadoGrupos.some(function (g) { return g.valor !== ''; });
            var selectActivo = selects.some(function (s) { return s.value !== ''; });
            var fechaActiva = fechas.some(function (f) { return f.value !== ''; });
            return pildoraActiva || selectActivo || fechaActiva;
        }

        // ---------- Aplicar TODOS los criterios a la vez ----------
        function aplicar() {
            var terminos = buscador ? normalizar(buscador.value).split(' ').filter(Boolean) : [];
            var visibles = 0;

            items.forEach(function (item) {
                var texto = textoPorItem.get(item);

                var coincide = terminos.every(function (t) { return texto.indexOf(t) !== -1; });

                if (coincide) {
                    coincide = estadoGrupos.every(function (g) {
                        return g.valor === '' || valorDe(item, g.clave) === g.valor;
                    });
                }
                if (coincide) {
                    coincide = selects.every(function (s) {
                        return s.value === '' || valorDe(item, s.getAttribute('data-filter-field')) === s.value;
                    });
                }
                if (coincide) {
                    coincide = fechas.every(function (f) {
                        return f.value === '' || valorDe(item, f.getAttribute('data-filter-date')) === f.value;
                    });
                }

                item.classList.toggle(CLASE_OCULTO, !coincide);
                if (coincide) {
                    visibles++;
                }
            });

            var filtrando = hayFiltrosActivos(terminos);

            // El mensaje "sin coincidencias" solo aplica si había datos; si el
            // servidor no envió registros, ya se muestra su propio estado vacío.
            if (mensajeVacio) {
                mensajeVacio.classList.toggle(CLASE_OCULTO, !(items.length > 0 && visibles === 0));
            }

            if (resultado) {
                resultado.textContent = filtrando
                    ? 'Mostrando ' + visibles + ' de ' + items.length
                    : '';
                resultado.classList.toggle(CLASE_OCULTO, !filtrando);
            }

            botonesLimpiar.forEach(function (b) {
                b.disabled = !filtrando;
            });
        }

        function limpiar() {
            if (buscador) {
                buscador.value = '';
            }
            selects.forEach(function (s) { s.value = ''; });
            fechas.forEach(function (f) { f.value = ''; });
            estadoGrupos.forEach(function (g) {
                g.valor = g.porDefecto ? g.porDefecto.getAttribute('data-filter-value') : '';
                pintarPildoras(g);
            });
            aplicar();
            if (buscador) {
                buscador.focus();
            }
        }

        // ---------- Eventos ----------
        if (buscador) {
            var temporizador = null;
            buscador.addEventListener('input', function () {
                window.clearTimeout(temporizador);
                temporizador = window.setTimeout(aplicar, ESPERA_BUSQUEDA_MS);
            });
            // Escape borra la búsqueda; Enter no debe enviar ningún formulario.
            buscador.addEventListener('keydown', function (e) {
                if (e.key === 'Escape' && buscador.value !== '') {
                    buscador.value = '';
                    aplicar();
                } else if (e.key === 'Enter') {
                    e.preventDefault();
                    window.clearTimeout(temporizador);
                    aplicar();
                }
            });
        }

        estadoGrupos.forEach(function (g) {
            pintarPildoras(g);
            g.botones.forEach(function (boton) {
                boton.addEventListener('click', function (e) {
                    e.preventDefault();
                    g.valor = boton.getAttribute('data-filter-value');
                    pintarPildoras(g);
                    aplicar();
                });
            });
        });

        selects.forEach(function (s) { s.addEventListener('change', aplicar); });
        fechas.forEach(function (f) {
            f.addEventListener('change', aplicar);
            f.addEventListener('input', aplicar);
        });

        botonesLimpiar.forEach(function (b) {
            b.addEventListener('click', function (e) {
                e.preventDefault();
                limpiar();
            });
        });

        // Estado inicial coherente (p. ej. si el navegador restauró el texto
        // del buscador al volver con el botón "Atrás").
        aplicar();
    }

    document.addEventListener('DOMContentLoaded', function () {
        aLista(document.querySelectorAll('[data-filter-scope]')).forEach(initScope);
    });
})();
