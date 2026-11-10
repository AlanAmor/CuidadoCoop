document.addEventListener('submit', function (event) {
    var mensaje = event.target.getAttribute && event.target.getAttribute('data-confirm');
    if (mensaje && !window.confirm(mensaje)) {
        event.preventDefault();
    }
});

(function () {
    function closeAllDropdowns(except) {
        document.querySelectorAll('.nav-dropdown.is-open').forEach(function (dd) {
            if (dd !== except) {
                dd.classList.remove('is-open');
                var btn = dd.querySelector('.nav-dropdown-toggle');
                if (btn) {
                    btn.setAttribute('aria-expanded', 'false');
                }
            }
        });
    }

    document.addEventListener('click', function (event) {
        var toggle = event.target.closest && event.target.closest('.nav-dropdown-toggle');
        if (toggle) {
            event.preventDefault();
            event.stopPropagation();
            var dropdown = toggle.closest('.nav-dropdown');
            var willOpen = !dropdown.classList.contains('is-open');
            closeAllDropdowns(dropdown);
            dropdown.classList.toggle('is-open', willOpen);
            toggle.setAttribute('aria-expanded', willOpen ? 'true' : 'false');
            return;
        }
        if (!event.target.closest || !event.target.closest('.nav-dropdown')) {
            closeAllDropdowns(null);
        }
    });

    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape') {
            closeAllDropdowns(null);
        }
    });
})();

(function () {
    function normalizeText(value) {
        return String(value || '')
            .toLowerCase()
            .normalize('NFD')
            .replace(/[\u0300-\u036f]/g, '')
            .trim();
    }

    function normalizeInitial(value) {
        var str = String(value || '').trim().toUpperCase();
        if (!str) {
            return '';
        }
        var ch = str.charAt(0);
        if (ch === 'Ñ') {
            return 'Ñ';
        }
        return ch.normalize('NFD').replace(/[\u0300-\u036f]/g, '');
    }

    function initIndexedTableSearch() {
        document.querySelectorAll('[data-indexed-search-form]').forEach(function (form) {
            var scope = form.getAttribute('data-indexed-search-form');
            var searchInput = form.querySelector('[data-search-input]');
            var initialInput = form.querySelector('[data-search-initial-input]');
            var espSelect = form.querySelector('[data-search-especialidad-select]');
            var estSelect = form.querySelector('[data-search-estado-select]');
            var azContainer = form.querySelector('[data-az-index]');
            var counterEl = form.querySelector('[data-search-counter]');
            var resetBtn = form.querySelector('[data-search-reset]');
            var rows = Array.prototype.slice.call(
                document.querySelectorAll('tr[data-search-row="' + scope + '"]')
            );
            var emptyRow = document.querySelector('tr[data-search-empty="' + scope + '"]');
            var entityLabel = (counterEl && counterEl.getAttribute('data-entity-label')) || scope;

            if (azContainer && rows.length > 0) {
                var availableLetters = {};
                rows.forEach(function (row) {
                    var ini = normalizeInitial(row.getAttribute('data-search-initial'));
                    if (ini) {
                        availableLetters[ini] = true;
                    }
                });
                azContainer.querySelectorAll('[data-letter]').forEach(function (btn) {
                    var letter = btn.getAttribute('data-letter');
                    if (letter && availableLetters[letter]) {
                        btn.classList.add('has-items');
                    }
                });
            }

            function applyFilter() {
                var query = normalizeText(searchInput ? searchInput.value : '');
                var terms = query ? query.split(/\s+/) : [];
                var activeInitial = normalizeInitial(initialInput ? initialInput.value : '');
                var activeEsp = espSelect ? espSelect.value : '';
                var activeEst = estSelect ? estSelect.value : '';

                var visibleCount = 0;
                rows.forEach(function (row) {
                    var rowText = normalizeText(row.getAttribute('data-search-text') || row.textContent);
                    var rowIni = normalizeInitial(row.getAttribute('data-search-initial'));
                    var rowEsp = row.getAttribute('data-search-especialidad') || '';
                    var rowEst = row.getAttribute('data-search-estado') || '';

                    var matchesTerms = terms.every(function (term) {
                        return rowText.indexOf(term) !== -1;
                    });
                    var matchesIni = !activeInitial || rowIni === activeInitial;
                    var matchesEsp = !activeEsp || rowEsp === activeEsp;
                    var matchesEst = !activeEst || rowEst === activeEst;

                    var show = matchesTerms && matchesIni && matchesEsp && matchesEst;
                    row.hidden = !show;
                    if (show) {
                        visibleCount++;
                    }
                });

                if (emptyRow) {
                    emptyRow.hidden = !(rows.length > 0 && visibleCount === 0);
                }
                if (counterEl && rows.length > 0) {
                    counterEl.textContent = 'Mostrando ' + visibleCount + ' de ' + rows.length + ' ' + entityLabel;
                }
                if (azContainer) {
                    azContainer.querySelectorAll('[data-letter]').forEach(function (btn) {
                        var letter = btn.getAttribute('data-letter') || '';
                        btn.classList.toggle('is-active', letter === activeInitial);
                    });
                }
            }

            if (searchInput) {
                searchInput.addEventListener('input', applyFilter);
            }
            if (espSelect) {
                espSelect.addEventListener('change', applyFilter);
            }
            if (estSelect) {
                estSelect.addEventListener('change', applyFilter);
            }
            if (azContainer) {
                azContainer.addEventListener('click', function (event) {
                    var pill = event.target.closest && event.target.closest('[data-letter]');
                    if (!pill) {
                        return;
                    }
                    event.preventDefault();
                    var letter = pill.getAttribute('data-letter') || '';
                    if (initialInput) {
                        initialInput.value = letter;
                    }
                    applyFilter();
                });
            }
            if (resetBtn) {
                resetBtn.addEventListener('click', function (event) {
                    var hasServerFilter = window.location.search && window.location.search.length > 1;
                    if (!hasServerFilter) {
                        event.preventDefault();
                        if (searchInput) searchInput.value = '';
                        if (initialInput) initialInput.value = '';
                        if (espSelect) espSelect.value = '';
                        if (estSelect) estSelect.value = '';
                        applyFilter();
                    }
                });
            }

            applyFilter();
        });
    }

    function initSearchableSelects() {
        document.querySelectorAll('select[data-searchable-select]').forEach(function (select) {
            if (select.dataset.searchableEnhanced === 'true') {
                return;
            }
            select.dataset.searchableEnhanced = 'true';

            var placeholder = select.getAttribute('data-searchable-select') || 'Buscar en la lista...';
            var wrapper = document.createElement('div');
            wrapper.className = 'searchable-select-box';

            var input = document.createElement('input');
            input.type = 'search';
            input.className = 'searchable-select-input';
            input.placeholder = placeholder;
            input.autocomplete = 'off';
            input.setAttribute('aria-label', placeholder);

            var status = document.createElement('span');
            status.className = 'searchable-select-count';

            wrapper.appendChild(input);
            wrapper.appendChild(status);
            select.parentNode.insertBefore(wrapper, select);

            var allOptions = Array.prototype.slice.call(select.options).map(function (opt) {
                return {
                    value: opt.value,
                    text: opt.text,
                    normalized: normalizeText(opt.text),
                    isPlaceholder: opt.value === ''
                };
            });

            function updateOptions() {
                var query = normalizeText(input.value);
                var terms = query ? query.split(/\s+/) : [];
                var currentValue = select.value;
                var matchedValues = [];

                select.innerHTML = '';
                allOptions.forEach(function (item) {
                    var matches = item.isPlaceholder || terms.every(function (t) {
                        return item.normalized.indexOf(t) !== -1;
                    });
                    if (matches) {
                        var opt = document.createElement('option');
                        opt.value = item.value;
                        opt.textContent = item.text;
                        select.appendChild(opt);
                        if (!item.isPlaceholder) {
                            matchedValues.push(item.value);
                        }
                    }
                });

                var totalReal = allOptions.filter(function (o) { return !o.isPlaceholder; }).length;
                if (query) {
                    status.textContent = matchedValues.length + ' de ' + totalReal;
                    if (matchedValues.length === 1 && select.value !== matchedValues[0]) {
                        select.value = matchedValues[0];
                        select.dispatchEvent(new Event('change', { bubbles: true }));
                    } else if (matchedValues.indexOf(currentValue) !== -1) {
                        select.value = currentValue;
                    } else {
                        select.value = '';
                    }
                } else {
                    status.textContent = totalReal + ' registrados';
                    select.value = currentValue;
                }
            }

            input.addEventListener('input', updateOptions);
            updateOptions();
        });
    }

    function initQuickTableFilters() {
        document.querySelectorAll('[data-quick-table-filter]').forEach(function (input) {
            var tableId = input.getAttribute('data-quick-table-filter');
            var table = document.getElementById(tableId);
            if (!table) {
                return;
            }
            input.addEventListener('input', function () {
                var query = normalizeText(input.value);
                var terms = query ? query.split(/\s+/) : [];
                var rows = table.querySelectorAll('tbody tr');
                rows.forEach(function (row) {
                    if (row.querySelector('.empty-state')) {
                        return;
                    }
                    var txt = normalizeText(row.textContent);
                    var show = terms.every(function (t) {
                        return txt.indexOf(t) !== -1;
                    });
                    row.hidden = !show;
                });
            });
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', function () {
            initIndexedTableSearch();
            initSearchableSelects();
            initQuickTableFilters();
        });
    } else {
        initIndexedTableSearch();
        initSearchableSelects();
        initQuickTableFilters();
    }
})();
