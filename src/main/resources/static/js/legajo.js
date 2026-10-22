(function () {
    'use strict';

    var MAX_BYTES = 10 * 1024 * 1024;
    var EXTENSIONES = /\.(pdf|png|jpe?g)$/i;

    function el(tag, className, text) {
        var nodo = document.createElement(tag);
        if (className) { nodo.className = className; }
        if (text !== undefined) { nodo.textContent = text; }
        return nodo;
    }

    function formatearTamano(bytes) {
        if (bytes < 1024) { return bytes + ' B'; }
        if (bytes < 1024 * 1024) { return (bytes / 1024).toFixed(0) + ' KB'; }
        return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
    }

    function formatearFecha(texto) {
        var fecha = new Date(texto);
        return isNaN(fecha) ? '' : fecha.toLocaleDateString('es-AR') + ' ' +
            fecha.toLocaleTimeString('es-AR', { hour: '2-digit', minute: '2-digit' });
    }

    function leerRespuesta(respuesta) {
        return respuesta.text().then(function (texto) {
            var datos = null;
            try { datos = texto ? JSON.parse(texto) : null; } catch (e) { datos = null; }
            if (!respuesta.ok) {
                var mensaje = datos && datos.error ? datos.error
                    : (respuesta.status === 413
                        ? 'El archivo es demasiado grande. Cada archivo puede pesar hasta 10 MB.'
                        : 'No se pudo completar la operación (código ' + respuesta.status + '). Intentá de nuevo en unos instantes.');
                throw new Error(mensaje);
            }
            return datos;
        }, function () {
            throw new Error('No se pudo contactar con el servidor. Revisá tu conexión e intentá de nuevo.');
        });
    }

    function pedir(url, opciones) {
        return fetch(url, opciones).then(leerRespuesta, function () {
            throw new Error('No se pudo contactar con el servidor. Revisá tu conexión e intentá de nuevo.');
        });
    }

    function urlLegajo(tipo, id) {
        return '/api/legajos/' + encodeURIComponent(tipo) + '/' + encodeURIComponent(id);
    }

    /* ---------- Ventana flotante de lectura ---------- */

    var ventana = null;
    var ultimaSolicitud = 0;

    function crearVentana() {
        var contenedor = el('div', 'legajo-ventana');
        contenedor.setAttribute('role', 'dialog');
        contenedor.setAttribute('aria-label', 'Legajo');
        contenedor.hidden = true;

        var cabecera = el('div', 'legajo-ventana-cabecera');
        var titulo = el('strong', 'legajo-ventana-titulo', 'Legajo');
        var cerrar = el('button', 'legajo-cerrar', '×');
        cerrar.type = 'button';
        cerrar.setAttribute('aria-label', 'Cerrar legajo');
        cabecera.appendChild(titulo);
        cabecera.appendChild(cerrar);

        var cuerpo = el('div', 'legajo-ventana-cuerpo');
        var lista = el('ul', 'legajo-lista');
        var visor = el('div', 'legajo-visor');
        cuerpo.appendChild(lista);
        cuerpo.appendChild(visor);

        contenedor.appendChild(cabecera);
        contenedor.appendChild(cuerpo);
        document.body.appendChild(contenedor);

        cerrar.addEventListener('click', cerrarVentana);
        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape' && !contenedor.hidden) { cerrarVentana(); }
        });
        hacerArrastrable(contenedor, cabecera);

        return { contenedor: contenedor, titulo: titulo, lista: lista, visor: visor };
    }

    function hacerArrastrable(contenedor, asa) {
        var desplazamiento = null;
        asa.addEventListener('pointerdown', function (e) {
            if (e.target.closest('button')) { return; }
            var caja = contenedor.getBoundingClientRect();
            desplazamiento = { x: e.clientX - caja.left, y: e.clientY - caja.top };
            contenedor.style.left = caja.left + 'px';
            contenedor.style.top = caja.top + 'px';
            contenedor.style.transform = 'none';
            asa.setPointerCapture(e.pointerId);
        });
        asa.addEventListener('pointermove', function (e) {
            if (!desplazamiento) { return; }
            var x = Math.min(Math.max(0, e.clientX - desplazamiento.x), window.innerWidth - 80);
            var y = Math.min(Math.max(0, e.clientY - desplazamiento.y), window.innerHeight - 40);
            contenedor.style.left = x + 'px';
            contenedor.style.top = y + 'px';
        });
        asa.addEventListener('pointerup', function () { desplazamiento = null; });
    }

    function cerrarVentana() {
        if (!ventana) { return; }
        ventana.contenedor.hidden = true;
            ultimaSolicitud++;
            liberarUrl();
            ventana.visor.replaceChildren();
    }

    function mostrarMensajeVisor(texto) {
        ventana.visor.replaceChildren(el('p', 'legajo-vacio', texto));
    }

    var urlActual = null;

    function liberarUrl() {
        if (urlActual) {
            URL.revokeObjectURL(urlActual);
            urlActual = null;
        }
    }

    function verArchivo(archivo, itemSeleccionado) {
        Array.prototype.forEach.call(ventana.lista.children, function (li) {
            li.classList.toggle('activo', li === itemSeleccionado);
        });
        var url = '/legajos/archivos/' + archivo.id;
        var esImagen = archivo.tipoContenido.indexOf('image/') === 0;

        var enlaces = el('div', 'legajo-visor-acciones');
        var abrir = el('a', 'button button-secondary', 'Abrir en pestaña nueva');
        abrir.href = url;
        abrir.target = '_blank';
        abrir.rel = 'noopener';
        var descargar = el('a', 'button button-secondary', 'Descargar');
        descargar.href = url + '?descargar=true';
        enlaces.appendChild(abrir);
        enlaces.appendChild(descargar);

        liberarUrl();
        var solicitud = ++ultimaSolicitud;
        ventana.visor.replaceChildren(enlaces, el('p', 'legajo-vacio', 'Cargando archivo…'));

        // Se descarga el archivo y se muestra desde una URL local: así no depende de las cabeceras
        // ni del visor de PDF integrado en un iframe, que varía entre Edge, Chrome y Firefox.
        fetch(url).then(function (respuesta) {
            if (!respuesta.ok) { throw new Error('http'); }
            return respuesta.blob();
        }).then(function (blob) {
            if (solicitud !== ultimaSolicitud) { return; }
            urlActual = URL.createObjectURL(new Blob([blob], { type: archivo.tipoContenido }));
            var elemento;
            if (esImagen) {
                elemento = el('img', 'legajo-imagen');
                elemento.alt = archivo.nombre;
                elemento.src = urlActual;
            } else {
                elemento = el('object', 'legajo-pdf');
                elemento.type = 'application/pdf';
                elemento.data = urlActual;
                elemento.setAttribute('aria-label', archivo.nombre);
                var alternativa = el('p', 'legajo-vacio',
                    'Tu navegador no puede mostrar este PDF dentro de la ventana. Usá "Abrir en pestaña nueva" o "Descargar".');
                elemento.appendChild(alternativa);
            }
            ventana.visor.replaceChildren(enlaces, elemento);
        }).catch(function () {
            if (solicitud !== ultimaSolicitud) { return; }
            ventana.visor.replaceChildren(enlaces, el('p', 'legajo-vacio',
                'No se pudo cargar el archivo. Puede que haya sido eliminado; cerrá y volvé a abrir el legajo.'));
        });
    }


    function abrirVentana(tipo, id, nombre) {
        if (!ventana) { ventana = crearVentana(); }
        ventana.titulo.textContent = 'Legajo · ' + nombre;
        ventana.lista.replaceChildren();
        mostrarMensajeVisor('Cargando archivos…');
        ventana.contenedor.hidden = false;

        pedir(urlLegajo(tipo, id)).then(function (archivos) {
            if (!archivos.length) {
                mostrarMensajeVisor('Este legajo todavía no tiene archivos. Podés cargarlos desde "Editar".');
                return;
            }
            archivos.forEach(function (archivo) {
                var item = el('li', 'legajo-item');
                var boton = el('button', 'legajo-item-boton');
                boton.type = 'button';
                boton.appendChild(el('span', 'legajo-item-nombre', archivo.nombre));
                boton.appendChild(el('small', 'legajo-item-detalle',
                    formatearFecha(archivo.fechaSubida) + ' · ' + formatearTamano(archivo.tamano)));
                boton.addEventListener('click', function () { verArchivo(archivo, item); });
                item.appendChild(boton);
                ventana.lista.appendChild(item);
            });
            verArchivo(archivos[0], ventana.lista.firstChild);
        }).catch(function (error) {
            mostrarMensajeVisor(error.message);
        });
    }

    document.addEventListener('click', function (e) {
        var boton = e.target.closest('[data-legajo-tipo]');
        if (!boton || boton.closest('[data-legajo-panel]')) { return; }
        abrirVentana(boton.getAttribute('data-legajo-tipo'), boton.getAttribute('data-legajo-id'),
            boton.getAttribute('data-legajo-titulo') || '');
    });

    /* ---------- Panel de carga (menú de edición) ---------- */

    function iniciarPanel(panel) {
        var tipo = panel.getAttribute('data-tipo');
        var id = panel.getAttribute('data-id');
        var zona = panel.querySelector('[data-zona]');
        var entrada = panel.querySelector('[data-entrada]');
        var estado = panel.querySelector('[data-estado]');
        var lista = panel.querySelector('[data-archivos]');

        function avisar(texto, esError) {
            estado.textContent = texto;
            estado.className = 'legajo-estado ' + (esError ? 'legajo-estado-error' : 'legajo-estado-ok');
            estado.hidden = !texto;
        }

        function refrescar() {
            return pedir(urlLegajo(tipo, id)).then(function (archivos) {
                lista.replaceChildren();
                if (!archivos.length) {
                    lista.appendChild(el('li', 'legajo-vacio', 'Todavía no hay archivos en este legajo.'));
                    return;
                }
                archivos.forEach(function (archivo) {
                    var item = el('li', 'legajo-fila');
                    var enlace = el('a', 'legajo-fila-nombre', archivo.nombre);
                    enlace.href = '/legajos/archivos/' + archivo.id;
                    enlace.target = '_blank';
                    enlace.rel = 'noopener';
                    var detalle = el('small', 'muted',
                        formatearFecha(archivo.fechaSubida) + ' · ' + formatearTamano(archivo.tamano));
                    var borrar = el('button', 'button button-danger', 'Eliminar');
                    borrar.type = 'button';
                    borrar.addEventListener('click', function () {
                        if (!window.confirm('¿Eliminar "' + archivo.nombre + '" del legajo? Esta acción no se puede deshacer.')) {
                            return;
                        }
                        pedir('/api/legajos/archivos/' + archivo.id, { method: 'DELETE' })
                            .then(function (r) { avisar(r.mensaje, false); return refrescar(); })
                            .catch(function (error) { avisar(error.message, true); });
                    });
                    item.appendChild(enlace);
                    item.appendChild(detalle);
                    item.appendChild(borrar);
                    lista.appendChild(item);
                });
            }).catch(function (error) { avisar(error.message, true); });
        }

        function subir(archivos) {
            var elegidos = Array.prototype.slice.call(archivos);
            if (!elegidos.length) { return; }
            for (var i = 0; i < elegidos.length; i++) {
                if (!EXTENSIONES.test(elegidos[i].name)) {
                    avisar('"' + elegidos[i].name + '" no es un archivo admitido. Solo se aceptan PDF, JPG y PNG.', true);
                    return;
                }
                if (elegidos[i].size > MAX_BYTES) {
                    avisar('"' + elegidos[i].name + '" supera el máximo de 10 MB.', true);
                    return;
                }
                if (elegidos[i].size === 0) {
                    avisar('"' + elegidos[i].name + '" está vacío.', true);
                    return;
                }
            }
            var datos = new FormData();
            elegidos.forEach(function (archivo) { datos.append('archivos', archivo); });
            zona.classList.add('subiendo');
            avisar('Subiendo ' + elegidos.length + (elegidos.length === 1 ? ' archivo…' : ' archivos…'), false);
            pedir(urlLegajo(tipo, id), { method: 'POST', body: datos })
                .then(function (r) { avisar(r.mensaje, false); return refrescar(); })
                .catch(function (error) { avisar(error.message, true); })
                .then(function () { zona.classList.remove('subiendo'); entrada.value = ''; });
        }

        ['dragenter', 'dragover'].forEach(function (nombre) {
            zona.addEventListener(nombre, function (e) { e.preventDefault(); zona.classList.add('arrastrando'); });
        });
        ['dragleave', 'drop'].forEach(function (nombre) {
            zona.addEventListener(nombre, function (e) { e.preventDefault(); zona.classList.remove('arrastrando'); });
        });
        zona.addEventListener('drop', function (e) { subir(e.dataTransfer.files); });
        zona.addEventListener('click', function () { entrada.click(); });
        zona.addEventListener('keydown', function (e) {
            if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); entrada.click(); }
        });
        entrada.addEventListener('change', function () { subir(entrada.files); });

        refrescar();
    }

    // Evita que soltar un archivo fuera de la zona haga que el navegador abra el archivo y se pierda la edición.
    ['dragover', 'drop'].forEach(function (nombre) {
        window.addEventListener(nombre, function (e) {
            if (!e.target.closest || !e.target.closest('[data-zona]')) { e.preventDefault(); }
        });
    });

    document.addEventListener('DOMContentLoaded', function () {
        Array.prototype.forEach.call(document.querySelectorAll('[data-legajo-panel]'), iniciarPanel);
    });
})();
