document.addEventListener('submit', function (event) {
    var mensaje = event.target.getAttribute && event.target.getAttribute('data-confirm');
    if (mensaje && !window.confirm(mensaje)) {
        event.preventDefault();
    }
});
