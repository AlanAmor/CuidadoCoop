(() => {
    const grid = document.getElementById("calendar-grid");
    if (!grid) {
        return;
    }

    const periodLabel = document.getElementById("calendar-period");
    const viewSelect = document.getElementById("calendar-view");
    const dateInput = document.getElementById("calendar-date");
    const errorBox = document.getElementById("calendar-error");
    const availablePartners = document.getElementById("available-partners");
    const unassignedServices = document.getElementById("unassigned-services");
    const availableCount = document.getElementById("available-count");
    const unassignedCount = document.getElementById("unassigned-service-count");
    const today = parseDate(dateInput.value);
    const HOUR_HEIGHT = 56;
    const detailBox = document.getElementById("calendar-detail");
    const notice = document.getElementById("calendar-notice");
    let anchor = new Date(today);
    let currentEvents = [];
    let activeRequest = 0;

    document.getElementById("calendar-prev").addEventListener("click", () => movePeriod(-1));
    document.getElementById("calendar-next").addEventListener("click", () => movePeriod(1));
    document.getElementById("calendar-today").addEventListener("click", () => {
        anchor = new Date(today);
        render();
    });
    viewSelect.addEventListener("change", render);
    dateInput.addEventListener("change", () => {
        if (dateInput.value) {
            anchor = parseDate(dateInput.value);
            render();
        }
    });

    render();

    function movePeriod(direction) {
        const view = viewSelect.value;
        if (view === "month") {
            anchor = new Date(anchor.getFullYear(), anchor.getMonth() + direction, 1);
        } else {
            anchor.setDate(anchor.getDate() + direction * (view === "week" ? 7 : 1));
        }
        render();
    }

    function render() {
        const view = viewSelect.value;
        const range = rangeFor(anchor, view);
        dateInput.value = formatDate(anchor);
        periodLabel.textContent = formatPeriod(anchor, view);
        loadCalendar(range, view);
    }

    function loadCalendar(range, view) {
        const request = ++activeRequest;
        errorBox.hidden = true;
        detailBox.hidden = true;
        grid.replaceChildren(createMessage("Consultando turnos…", "calendar-loading"));
        const params = new URLSearchParams({
            desde: formatDate(range.start),
            hasta: formatDate(range.end)
        });

        fetch(`/api/calendario?${params.toString()}`, {
            headers: {Accept: "application/json"}
        })
            .then(response => {
                if (!response.ok) {
                    throw new Error(response.status === 400
                        ? "El rango seleccionado no es válido. Elegí un período de hasta 63 días."
                        : "No se pudieron cargar los turnos. Actualizá la página o intentá nuevamente.");
                }
                return response.json();
            })
            .then(data => {
                if (request !== activeRequest) {
                    return;
                }
                currentEvents = data.eventos || [];
                renderCalendar(range, view, currentEvents);
                renderList(availablePartners, availableCount, data.sociosDisponibles || [],
                    item => {
                        const row = createElement("li", "availability-item availability-item-socio");
                        const socioSpan = createElement("strong", "availability-text-socio", item.nombre);
                        const sep = createElement("span", "availability-sep", " · ");
                        const espSpan = createElement("span", "availability-tag-servicio", label(item.especialidad));
                        row.append(socioSpan, sep, espSpan);
                        return row;
                    },
                    "No hay socios marcados como disponibles actualmente.");
                renderList(unassignedServices, unassignedCount, data.clientesSinTurno || [],
                    item => {
                        const row = createElement("li", "availability-item availability-item-domicilio");
                        const pacSpan = createElement("strong", "availability-text-paciente", item.paciente);
                        const sep = createElement("span", "availability-sep", " · ");
                        const domSpan = createElement("span", "availability-text-domicilio", item.domicilio);
                        row.append(pacSpan, sep, domSpan);
                        return row;
                    },
                    "Todos los clientes tienen turnos en el período.");
            })
            .catch(error => {
                if (request !== activeRequest) {
                    return;
                }
                grid.replaceChildren();
                errorBox.textContent = error.message;
                errorBox.hidden = false;
                renderList(availablePartners, availableCount, [],
                    () => "", "No se pudo consultar la disponibilidad.");
                renderList(unassignedServices, unassignedCount, [],
                    () => "", "No se pudo consultar los clientes.");
            });
    }

    function renderCalendar(range, view, events) {
        grid.replaceChildren();
        if (view === "month") {
            renderMonth(range, events);
        } else {
            renderTimeline(range, view, events);
        }
    }

    function renderTimeline(range, view, events) {
        const days = listDays(range.start, range.end);
        const table = document.createElement("div");
        table.className = `timeline-grid ${view === "day" ? "timeline-day" : ""}`;
        table.style.setProperty("--day-count", days.length);
        table.appendChild(createElement("div", "timeline-corner", "Hora"));
        days.forEach(day => {
            const heading = createElement("button", "timeline-day-heading");
            heading.type = "button";
            heading.textContent = new Intl.DateTimeFormat("es-AR", {
                weekday: "short", day: "numeric", month: "short"
            }).format(day);
            if (sameDay(day, today)) {
                heading.classList.add("is-today");
            }
            heading.addEventListener("click", () => {
                anchor = new Date(day);
                viewSelect.value = "day";
                render();
            });
            table.appendChild(heading);
        });

        // Cada hora y cada día se ubican de forma explícita para que ningún turno desplace la grilla.
        for (let hour = 0; hour < 24; hour++) {
            const label = createElement("div", "timeline-hour", `${String(hour).padStart(2, "0")}:00`);
            label.style.gridColumn = "1";
            label.style.gridRow = String(hour + 2);
            table.appendChild(label);
        }

        let primeraHora = null;
        days.forEach((day, dayIndex) => {
            const column = createElement("div", "timeline-day-column");
            column.style.gridColumn = String(dayIndex + 2);
            column.style.gridRow = "2 / span 24";
            if (sameDay(day, today)) {
                column.classList.add("is-today");
            }
            const inicioDia = startOfDay(day);
            const finDia = new Date(inicioDia);
            finDia.setDate(finDia.getDate() + 1);

            const segments = events
                .filter(event => new Date(event.inicio) < finDia && new Date(event.fin) > inicioDia)
                .map(event => {
                    const inicioEvento = new Date(event.inicio);
                    const finEvento = new Date(event.fin);
                    const inicioVisible = inicioEvento < inicioDia ? inicioDia : inicioEvento;
                    const finVisible = finEvento > finDia ? finDia : finEvento;
                    const startMinute = Math.round((inicioVisible - inicioDia) / 60000);
                    const endMinute = Math.round((finVisible - inicioDia) / 60000);
                    return {event, inicioEvento, inicioDia, inicioVisible, finVisible, finDia, startMinute, endMinute};
                })
                .sort((a, b) => a.startMinute - b.startMinute || a.endMinute - b.endMinute);

            assignLanes(segments).forEach(segment => {
                column.appendChild(createEventCard(segment));
                const hour = Math.floor(segment.startMinute / 60);
                primeraHora = primeraHora === null ? hour : Math.min(primeraHora, hour);
            });
            table.appendChild(column);
        });
        grid.appendChild(table);
        grid.scrollTop = Math.max(0, (primeraHora === null ? 7 : Math.max(0, primeraHora - 1)) * HOUR_HEIGHT);
    }

    function assignLanes(segments) {
        let cluster = [];
        let clusterEnd = 0;
        const closeCluster = () => {
            const lanes = cluster.reduce((max, segment) => Math.max(max, segment.lane + 1), 1);
            cluster.forEach(segment => {
                segment.lanes = lanes;
            });
            cluster = [];
        };

        segments.forEach(segment => {
            if (cluster.length && segment.startMinute >= clusterEnd) {
                closeCluster();
            }
            const laneEnds = [];
            cluster.forEach(other => {
                laneEnds[other.lane] = Math.max(laneEnds[other.lane] || 0, other.endMinute);
            });
            let lane = laneEnds.findIndex(end => end <= segment.startMinute);
            if (lane < 0) {
                lane = laneEnds.length;
            }
            segment.lane = lane;
            cluster.push(segment);
            clusterEnd = cluster.length === 1 ? segment.endMinute : Math.max(clusterEnd, segment.endMinute);
        });
        if (cluster.length) {
            closeCluster();
        }
        return segments;
    }

    function createEventCard(segment) {
        const {event, inicioEvento, inicioDia, inicioVisible, finVisible, finDia} = segment;
        const card = createElement("button", "calendar-event");
        card.type = "button";
        card.style.top = `${segment.startMinute / 1440 * 100}%`;
        card.style.height = `${Math.max(segment.endMinute - segment.startMinute, 30) / 1440 * 100}%`;
        card.style.left = `calc(${segment.lane / segment.lanes * 100}% + 2px)`;
        card.style.width = `calc(${100 / segment.lanes}% - 4px)`;
        card.title = `${event.nombreSocio} · ${event.paciente} · DNI ${event.dniSocio}`;
        const continuacion = inicioEvento < inicioDia ? "Continúa · " : "";
        const horaFinVisible = finVisible.getTime() === finDia.getTime() ? "24:00" : time(finVisible);
        card.appendChild(createElement(
            "strong", "", `${continuacion}${time(inicioVisible)}–${horaFinVisible}`));
        card.appendChild(createElement("span", "", event.paciente));
        card.appendChild(createElement("span", "calendar-event-meta", event.nombreSocio));
        card.appendChild(createElement("span", "calendar-event-address", event.domicilio));
        card.addEventListener("click", () => showDetail(event));
        return card;
    }

    function showDetail(event) {
        detailBox.replaceChildren();
        detailBox.hidden = false;
        const title = createElement("h3", "detail-title", event.paciente);
        const items = [
            {
                className: "detail-item detail-item-socio",
                label: "Socio:",
                value: `${event.nombreSocio} · DNI ${event.dniSocio}`
            },
            {
                className: "detail-item detail-item-domicilio",
                label: "Domicilio:",
                value: event.domicilio
            },
            {
                className: "detail-item detail-item-servicio",
                label: "Servicio:",
                value: label(event.tipoServicio)
            },
            {
                className: "detail-item detail-item-horario",
                label: "Horario:",
                value: `${time(event.inicio)} a ${time(event.fin)} · ${dateText(event.inicio)}`
            }
        ];
        const itemsContainer = createElement("div", "detail-items");
        items.forEach(item => {
            const p = createElement("p", item.className);
            const strong = createElement("strong", "detail-item-label", `${item.label} `);
            const span = createElement("span", "detail-item-value", item.value);
            p.append(strong, span);
            itemsContainer.appendChild(p);
        });
        detailBox.append(title, itemsContainer);

        const actions = createElement("div", "form-actions");
        const close = createElement("button", "button button-quiet", "Cerrar");
        close.type = "button";
        close.addEventListener("click", () => {
            detailBox.hidden = true;
        });
        const form = document.createElement("form");
        form.method = "post";
        form.action = `/asistencias/${event.asistenciaId}/eliminar`;
        form.dataset.confirm = "¿Eliminar este turno? Esta acción no se puede deshacer.";
        form.addEventListener("submit", submitEvent => {
            if (!window.confirm(form.dataset.confirm)) {
                submitEvent.preventDefault();
            }
        });
        const remove = createElement("button", "button button-danger", "Eliminar turno");
        remove.type = "submit";
        form.appendChild(remove);
        const edit = createElement("button", "button button-primary", "Editar turno");
        edit.type = "button";
        edit.addEventListener("click", () => showEditForm(event));
        actions.append(close, edit, form);
        detailBox.appendChild(actions);
        detailBox.scrollIntoView({behavior: "smooth", block: "nearest"});
    }

    function showEditForm(event) {
        detailBox.replaceChildren();
        detailBox.appendChild(createElement("h3", "", `Editar turno · ${event.paciente}`));
        const message = createElement("div", "alert alert-error");
        message.hidden = true;
        message.setAttribute("role", "alert");

        const dateField = createInput("Fecha", "date", event.fecha);
        const startField = createInput("Hora de inicio", "time", time(event.inicio));
        const endField = createInput("Hora de fin", "time", time(event.fin));
        const partnerField = createCustomPartnerDropdown("Socio disponible en ese horario");
        const clientField = createSelect("Cliente y domicilio");
        const serviceField = createSelect("Servicio");

        const timeGrid = createElement("div", "form-grid");
        timeGrid.append(dateField.wrapper, startField.wrapper, endField.wrapper);
        const hint = createElement("p", "form-hint",
            "Al cambiar el día o el horario, la lista de socios se actualiza con los que están libres en ese momento.");

        const actions = createElement("div", "form-actions");
        const cancel = createElement("button", "button button-quiet", "Cancelar");
        cancel.type = "button";
        cancel.addEventListener("click", () => showDetail(event));
        const save = createElement("button", "button button-primary", "Guardar cambios");
        save.type = "button";
        actions.append(cancel, save);
        detailBox.append(message, timeGrid, partnerField.wrapper, clientField.wrapper, serviceField.wrapper, hint, actions);

        const showError = text => {
            message.textContent = text;
            message.hidden = !text;
        };

        let partnerRequest = 0;
        const loadPartners = selectedId => {
            const request = ++partnerRequest;
            if (!dateField.input.value || !startField.input.value || !endField.input.value) {
                partnerField.setPlaceholder("Completá día y horario");
                return;
            }
            const params = new URLSearchParams({
                fecha: dateField.input.value,
                horaInicio: startField.input.value,
                horaFin: endField.input.value,
                excluirAsistenciaId: event.asistenciaId
            });
            fetch(`/api/socios/disponibles?${params}`, {headers: {Accept: "application/json"}})
                .then(response => response.json().then(body => ({ok: response.ok, body})))
                .then(({ok, body}) => {
                    if (request !== partnerRequest) {
                        return;
                    }
                    if (!ok) {
                        partnerField.setPlaceholder("No hay socios para mostrar");
                        showError(body.error || "No se pudo consultar la disponibilidad.");
                        return;
                    }
                    showError("");
                    partnerField.updateOptions(
                        body,
                        body.length ? "Seleccioná un socio" : "Todos los socios están ocupados en ese horario",
                        selectedId);
                })
                .catch(() => showError("No se pudo consultar la disponibilidad. Intentá nuevamente."));
        };
        [dateField, startField, endField].forEach(field => field.input.addEventListener("change", () => {
            const current = partnerField.select.value;
            loadPartners(current ? Number(current) : null);
        }));

        const loadOptions = (url, select, mapper, placeholder, selectedId) => fetch(url, {headers: {Accept: "application/json"}})
            .then(response => response.json())
            .then(items => fillSelect(select, items.map(mapper), placeholder, selectedId))
            .catch(() => showError("No se pudieron cargar las opciones. Actualizá la página."));
        loadOptions("/api/clientes/opciones", clientField.select,
            item => ({value: item.clienteId, text: `${item.paciente} · ${item.domicilio}`}),
            "Seleccioná un cliente", event.clienteId);
        loadOptions("/api/servicios/opciones", serviceField.select,
            item => ({
                value: item.servicioId,
                text: `${label(item.tipoServicio)} · cliente $ ${item.valorHora}/h · socio $ ${item.valorHoraSocio}/h`
            }),
            "Seleccioná un servicio", event.servicioId);
        loadPartners(event.socioId);

        save.addEventListener("click", () => {
            showError("");
            if (!partnerField.select.value) {
                partnerField.toggle.setAttribute("aria-invalid", "true");
            }
            save.disabled = true;
            fetch(`/api/asistencias/${event.asistenciaId}`, {
                method: "POST",
                headers: {"Content-Type": "application/json", Accept: "application/json"},
                body: JSON.stringify({
                    socioId: partnerField.select.value ? Number(partnerField.select.value) : null,
                    clienteId: clientField.select.value ? Number(clientField.select.value) : null,
                    servicioId: serviceField.select.value ? Number(serviceField.select.value) : null,
                    fecha: dateField.input.value || null,
                    horaInicio: startField.input.value || null,
                    horaFin: endField.input.value || null
                })
            })
                .then(response => response.json().then(body => ({ok: response.ok, body})))
                .then(({ok, body}) => {
                    if (!ok) {
                        throw new Error(body.error || "No se pudo guardar el turno. Revisá los datos e intentá nuevamente.");
                    }
                    notice.textContent = body.mensaje;
                    notice.hidden = false;
                    setTimeout(() => {
                        notice.hidden = true;
                    }, 6000);
                    anchor = parseDate(dateField.input.value);
                    render();
                })
                .catch(error => {
                    showError(error.message);
                    save.disabled = false;
                });
        });
        detailBox.scrollIntoView({behavior: "smooth", block: "nearest"});
    }

    function createInput(text, type, value) {
        const wrapper = createElement("div", "field");
        const input = document.createElement("input");
        input.type = type;
        input.value = value;
        input.required = true;
        const labelElement = createElement("label", "", text);
        wrapper.append(labelElement, input);
        return {wrapper, input};
    }

    function createSelect(text) {
        const wrapper = createElement("div", "field field-spaced");
        const select = document.createElement("select");
        wrapper.append(createElement("label", "", text), select);
        return {wrapper, select};
    }

    function createCustomPartnerDropdown(text) {
        const wrapper = createElement("div", "field field-spaced");
        const labelEl = createElement("label", "", text);
        labelEl.htmlFor = "edit-turno-socio-toggle";
        wrapper.appendChild(labelEl);

        const container = createElement("div", "custom-dropdown");
        const select = document.createElement("select");
        select.className = "custom-dropdown-native";
        select.id = "edit-turno-socio-native";
        select.name = "socioId";

        const toggle = createElement("button", "custom-dropdown-toggle");
        toggle.id = "edit-turno-socio-toggle";
        toggle.type = "button";
        toggle.setAttribute("role", "combobox");
        toggle.setAttribute("aria-haspopup", "listbox");
        toggle.setAttribute("aria-expanded", "false");
        toggle.setAttribute("aria-controls", "edit-turno-socio-menu");
        toggle.setAttribute("aria-invalid", "false");

        const labelSpan = createElement("span", "custom-dropdown-label", "Completá día y horario");
        const arrow = createElement("span", "custom-dropdown-arrow", "▾");
        arrow.setAttribute("aria-hidden", "true");
        toggle.append(labelSpan, arrow);

        const menu = createElement("div", "custom-dropdown-menu");
        menu.id = "edit-turno-socio-menu";
        menu.setAttribute("role", "listbox");
        menu.setAttribute("aria-labelledby", "edit-turno-socio-toggle");
        menu.setAttribute("tabindex", "-1");
        menu.hidden = true;

        let optionsData = [];
        let activeIndex = -1;

        function setActive(index, scroll = true) {
            const items = menu.querySelectorAll(".custom-dropdown-item");
            if (items.length === 0) {
                activeIndex = -1;
                toggle.removeAttribute("aria-activedescendant");
                return;
            }
            activeIndex = Math.max(0, Math.min(index, items.length - 1));
            items.forEach((el, idx) => {
                el.classList.toggle("is-active", idx === activeIndex);
            });
            const activeEl = items[activeIndex];
            if (activeEl) {
                toggle.setAttribute("aria-activedescendant", activeEl.id);
                if (scroll) {
                    activeEl.scrollIntoView({block: "nearest"});
                }
            }
        }

        function closeMenu() {
            menu.hidden = true;
            toggle.setAttribute("aria-expanded", "false");
            menu.querySelectorAll(".custom-dropdown-item").forEach(el => el.classList.remove("is-active"));
            toggle.removeAttribute("aria-activedescendant");
        }

        function openMenu() {
            if (toggle.disabled || optionsData.length === 0) return;
            menu.hidden = false;
            toggle.setAttribute("aria-expanded", "true");
            const currentIdx = optionsData.findIndex(it => Number(it.socioId) === Number(select.value));
            setActive(currentIdx >= 0 ? currentIdx : 0);
        }

        toggle.addEventListener("click", e => {
            e.stopPropagation();
            if (menu.hidden) {
                openMenu();
            } else {
                closeMenu();
            }
        });

        const onDocClick = e => {
            if (!document.contains(container)) {
                document.removeEventListener("click", onDocClick);
                return;
            }
            if (!container.contains(e.target)) {
                closeMenu();
            }
        };
        document.addEventListener("click", onDocClick);

        container.addEventListener("keydown", e => {
            if (e.key === "Escape") {
                closeMenu();
                toggle.focus();
            } else if (e.key === "ArrowDown") {
                e.preventDefault();
                if (menu.hidden) {
                    openMenu();
                } else if (optionsData.length > 0) {
                    const nextIdx = activeIndex < optionsData.length - 1 ? activeIndex + 1 : 0;
                    setActive(nextIdx);
                }
            } else if (e.key === "ArrowUp") {
                e.preventDefault();
                if (menu.hidden) {
                    openMenu();
                } else if (optionsData.length > 0) {
                    const prevIdx = activeIndex > 0 ? activeIndex - 1 : optionsData.length - 1;
                    setActive(prevIdx);
                }
            } else if (e.key === "Home" && !menu.hidden && optionsData.length > 0) {
                e.preventDefault();
                setActive(0);
            } else if (e.key === "End" && !menu.hidden && optionsData.length > 0) {
                e.preventDefault();
                setActive(optionsData.length - 1);
            } else if ((e.key === "Enter" || e.key === " ") && !menu.hidden) {
                e.preventDefault();
                if (activeIndex >= 0 && optionsData[activeIndex]) {
                    selectItem(optionsData[activeIndex].socioId);
                    closeMenu();
                    toggle.focus();
                }
            } else if (e.key === "Tab" && !menu.hidden) {
                closeMenu();
            }
        });

        function selectItem(socioId) {
            select.value = socioId !== null && socioId !== undefined ? String(socioId) : "";
            const found = optionsData.find(item => Number(item.socioId) === Number(socioId));
            if (found) {
                labelSpan.textContent = `${found.nombre} · ${label(found.especialidad)}`;
            } else if (optionsData.length === 0) {
                labelSpan.textContent = select.dataset.placeholder || "No hay socios para mostrar";
            } else {
                labelSpan.textContent = "Seleccioná un socio";
            }
            const items = menu.querySelectorAll(".custom-dropdown-item");
            items.forEach((el, idx) => {
                const isSel = Number(el.dataset.value) === Number(socioId);
                el.classList.toggle("is-selected", isSel);
                el.setAttribute("aria-selected", isSel ? "true" : "false");
                if (isSel) activeIndex = idx;
            });
            if (activeIndex >= 0 && items[activeIndex]) {
                toggle.setAttribute("aria-activedescendant", items[activeIndex].id);
            }
            toggle.setAttribute("aria-invalid", "false");
            select.dispatchEvent(new Event("change"));
        }

        function updateOptions(items, placeholder, selectedId) {
            optionsData = items || [];
            select.dataset.placeholder = placeholder;
            select.replaceChildren();

            const emptyOption = document.createElement("option");
            emptyOption.value = "";
            emptyOption.textContent = placeholder;
            select.appendChild(emptyOption);

            menu.replaceChildren();

            if (optionsData.length === 0) {
                toggle.disabled = true;
                labelSpan.textContent = placeholder;
                select.value = "";
                closeMenu();
                return;
            }

            toggle.disabled = false;
            optionsData.forEach(item => {
                const opt = document.createElement("option");
                opt.value = item.socioId;
                opt.textContent = `${item.nombre} · ${label(item.especialidad)}`;
                select.appendChild(opt);

                const itemDiv = createElement("div", "custom-dropdown-item");
                itemDiv.id = `edit-partner-opt-${item.socioId}`;
                itemDiv.setAttribute("role", "option");
                itemDiv.setAttribute("tabindex", "-1");
                itemDiv.dataset.value = item.socioId;
                const isSelected = selectedId !== null && selectedId !== undefined && Number(item.socioId) === Number(selectedId);
                itemDiv.setAttribute("aria-selected", isSelected ? "true" : "false");

                const textSpan = createElement("span", "custom-dropdown-item-text",
                    `${item.nombre} · ${label(item.especialidad)}`);
                itemDiv.appendChild(textSpan);

                if (item.tieneTurnoMismoDia) {
                    const warningSpan = createElement("span", "warning-badge-day", "Ya tiene un servicio ese día");
                    itemDiv.appendChild(warningSpan);
                }

                itemDiv.addEventListener("click", e => {
                    e.stopPropagation();
                    selectItem(item.socioId);
                    closeMenu();
                    toggle.focus();
                });

                menu.appendChild(itemDiv);
            });

            if (selectedId !== null && selectedId !== undefined && optionsData.some(it => Number(it.socioId) === Number(selectedId))) {
                selectItem(selectedId);
            } else {
                select.value = "";
                labelSpan.textContent = placeholder;
                menu.querySelectorAll(".custom-dropdown-item").forEach(el => {
                    el.classList.remove("is-selected");
                    el.setAttribute("aria-selected", "false");
                });
            }
        }

        container.append(select, toggle, menu);
        wrapper.appendChild(container);

        return {
            wrapper,
            select,
            toggle,
            updateOptions,
            setPlaceholder: (text) => updateOptions([], text, null)
        };
    }

    function fillSelect(select, options, placeholder, selectedId) {
        select.replaceChildren();
        const empty = document.createElement("option");
        empty.value = "";
        empty.textContent = placeholder;
        select.appendChild(empty);
        options.forEach(item => {
            const option = document.createElement("option");
            option.value = item.value;
            option.textContent = item.text;
            option.selected = selectedId !== null && Number(item.value) === Number(selectedId);
            select.appendChild(option);
        });
    }

    function dateText(value) {
        return new Intl.DateTimeFormat("es-AR", {dateStyle: "full"}).format(new Date(value));
    }

    function renderMonth(range, events) {
        const wrapper = createElement("div", "month-grid");
        ["Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"].forEach(day => {
            wrapper.appendChild(createElement("div", "month-weekday", day));
        });

        listDays(range.start, range.end).forEach(day => {
            const cell = createElement("section", "month-day");
            if (sameDay(day, today)) {
                cell.classList.add("is-today");
            }
            const dayButton = createElement("button", "month-day-number", String(day.getDate()));
            dayButton.type = "button";
            dayButton.setAttribute("aria-label", `Ver el ${formatDate(day)} por día`);
            dayButton.addEventListener("click", () => {
                anchor = new Date(day);
                viewSelect.value = "day";
                render();
            });
            cell.appendChild(dayButton);
            const daily = events.filter(event => eventoCoincideConDia(event, day));
            daily.slice(0, 3).forEach(event => cell.appendChild(createMonthEvent(event, day)));
            if (daily.length > 3) {
                cell.appendChild(createElement("span", "month-more", `+${daily.length - 3} más`));
            }
            wrapper.appendChild(cell);
        });
        grid.appendChild(wrapper);
    }

    function createMonthEvent(event, day) {
        const chip = createElement("button", "month-event");
        chip.type = "button";
        chip.title = `${event.nombreSocio} · ${event.domicilio}`;
        const inicioEvento = new Date(event.inicio);
        const inicioDia = startOfDay(day);
        const inicioVisible = inicioEvento < inicioDia ? inicioDia : inicioEvento;
        const continuacion = inicioEvento < inicioDia ? "Continúa · " : "";
        chip.appendChild(createElement("strong", "", `${continuacion}${time(inicioVisible)} ${event.paciente}`));
        chip.appendChild(createElement("span", "", event.nombreSocio));
        chip.addEventListener("click", () => showDetail(event));
        return chip;
    }

    function eventoCoincideConDia(event, day) {
        const inicio = new Date(event.inicio);
        const fin = new Date(event.fin);
        const inicioDia = startOfDay(day);
        const finDia = new Date(inicioDia);
        finDia.setDate(finDia.getDate() + 1);
        return inicio < finDia && fin > inicioDia;
    }

    function renderList(list, count, items, renderItem, emptyText) {
        count.textContent = String(items.length);
        list.replaceChildren();
        if (!items.length) {
            list.appendChild(createElement("li", "availability-empty", emptyText));
            return;
        }
        items.forEach(item => {
            const rendered = renderItem(item);
            if (rendered instanceof Node) {
                list.appendChild(rendered);
            } else {
                list.appendChild(createElement("li", "availability-item", rendered));
            }
        });
    }

    function rangeFor(date, view) {
        if (view === "day") {
            return {start: startOfDay(date), end: startOfDay(date)};
        }
        if (view === "week") {
            const start = startOfWeek(date);
            const end = new Date(start);
            end.setDate(end.getDate() + 6);
            return {start, end};
        }
        const first = new Date(date.getFullYear(), date.getMonth(), 1);
        const start = startOfWeek(first);
        const last = new Date(date.getFullYear(), date.getMonth() + 1, 0);
        const end = startOfWeek(last);
        end.setDate(end.getDate() + 6);
        return {start, end};
    }

    function startOfWeek(date) {
        const result = startOfDay(date);
        const offset = (result.getDay() + 6) % 7;
        result.setDate(result.getDate() - offset);
        return result;
    }

    function startOfDay(date) {
        return new Date(date.getFullYear(), date.getMonth(), date.getDate());
    }

    function listDays(start, end) {
        const result = [];
        for (const current = new Date(start); current <= end; current.setDate(current.getDate() + 1)) {
            result.push(new Date(current));
        }
        return result;
    }

    function formatPeriod(date, view) {
        if (view === "day") {
            return new Intl.DateTimeFormat("es-AR", {
                weekday: "long", day: "numeric", month: "long", year: "numeric"
            }).format(date);
        }
        if (view === "week") {
            const range = rangeFor(date, view);
            const start = new Intl.DateTimeFormat("es-AR", {day: "numeric", month: "short"}).format(range.start);
            const end = new Intl.DateTimeFormat("es-AR", {
                day: "numeric", month: "short", year: "numeric"
            }).format(range.end);
            return `${start} – ${end}`;
        }
        return new Intl.DateTimeFormat("es-AR", {month: "long", year: "numeric"}).format(date);
    }

    function parseDate(value) {
        const [year, month, day] = value.split("-").map(Number);
        return new Date(year, month - 1, day);
    }

    function formatDate(date) {
        return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
    }

    function time(value) {
        return new Intl.DateTimeFormat("es-AR", {
            hour: "2-digit", minute: "2-digit", hourCycle: "h23"
        }).format(new Date(value));
    }

    function label(value) {
        return value === "SERVICIO_24_7" ? "24/7"
            : value === "ENFERMERIA_EXPRESS" ? "Enfermería express"
                : value === "CUIDADOR" ? "Cuidador"
                    : value === "ENFERMERO" ? "Enfermero"
                        : value;
    }

    function sameDay(left, right) {
        return left.getFullYear() === right.getFullYear()
            && left.getMonth() === right.getMonth()
            && left.getDate() === right.getDate();
    }

    function createElement(tag, className, text) {
        const element = document.createElement(tag);
        if (className) {
            element.className = className;
        }
        if (text !== undefined) {
            element.textContent = text;
        }
        return element;
    }

    function createMessage(text, className) {
        return createElement("p", className, text);
    }
})();
