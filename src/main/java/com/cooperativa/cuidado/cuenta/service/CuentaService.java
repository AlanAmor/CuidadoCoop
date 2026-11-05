package com.cooperativa.cuidado.cuenta.service;

import com.cooperativa.cuidado.cuenta.Permiso;
import com.cooperativa.cuidado.cuenta.RolUsuario;
import com.cooperativa.cuidado.cuenta.RolUsuarioRepository;
import com.cooperativa.cuidado.cuenta.UsuarioCuenta;
import com.cooperativa.cuidado.cuenta.UsuarioCuentaRepository;
import com.cooperativa.cuidado.exception.DatosInvalidosException;
import com.cooperativa.cuidado.exception.RecursoDuplicadoException;
import com.cooperativa.cuidado.exception.RecursoNoEncontradoException;
import com.cooperativa.cuidado.exception.ReglaNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class CuentaService {

    private final UsuarioCuentaRepository usuarioRepository;
    private final RolUsuarioRepository rolRepository;
    private final PasswordHasher passwordHasher;

    public CuentaService(
            UsuarioCuentaRepository usuarioRepository,
            RolUsuarioRepository rolRepository,
            PasswordHasher passwordHasher) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordHasher = passwordHasher;
    }

    @Transactional(readOnly = true)
    public List<UsuarioCuenta> listarUsuarios() {
        return usuarioRepository.findAllByOrderByApellidoAscNombreAsc();
    }

    @Transactional(readOnly = true)
    public UsuarioCuenta obtenerUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La cuenta de usuario solicitada no existe."));
    }

    @Transactional(readOnly = true)
    public Optional<UsuarioCuenta> autenticar(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            return Optional.empty();
        }
        return usuarioRepository.findByUsernameIgnoreCase(username.trim())
                .filter(u -> passwordHasher.verify(password, u.getPasswordHash()));
    }

    @Transactional
    public UsuarioCuenta crearUsuario(
            String username,
            String rawPassword,
            String nombre,
            String apellido,
            String dni,
            Long rolId) {
        String userLimpio = limpiarTexto(username, "El nombre de usuario es obligatorio.");
        if (rawPassword == null || rawPassword.trim().length() < 4) {
            throw new DatosInvalidosException("La contraseña debe tener al menos 4 caracteres.");
        }
        String nombreLimpio = limpiarTexto(nombre, "El nombre es obligatorio.");
        String apellidoLimpio = limpiarTexto(apellido, "El apellido es obligatorio.");
        String dniLimpio = validarDni(dni);
        RolUsuario rol = obtenerRol(rolId);

        if (usuarioRepository.existsByUsernameIgnoreCase(userLimpio)) {
            throw new RecursoDuplicadoException("Ya existe una cuenta con el usuario \"" + userLimpio + "\"");
        }
        if (usuarioRepository.existsByDni(dniLimpio)) {
            throw new RecursoDuplicadoException("Ya existe una cuenta registrada con el DNI " + dniLimpio);
        }

        UsuarioCuenta cuenta = new UsuarioCuenta();
        cuenta.setUsername(userLimpio);
        cuenta.setPasswordHash(passwordHasher.hash(rawPassword.trim()));
        cuenta.setNombre(nombreLimpio);
        cuenta.setApellido(apellidoLimpio);
        cuenta.setDni(dniLimpio);
        cuenta.setRol(rol);
        return usuarioRepository.save(cuenta);
    }

    @Transactional
    public UsuarioCuenta actualizarUsuario(
            Long id,
            String username,
            String rawPassword,
            String nombre,
            String apellido,
            String dni,
            Long rolId) {
        UsuarioCuenta cuenta = obtenerUsuario(id);
        String userLimpio = limpiarTexto(username, "El nombre de usuario es obligatorio.");
        String nombreLimpio = limpiarTexto(nombre, "El nombre es obligatorio.");
        String apellidoLimpio = limpiarTexto(apellido, "El apellido es obligatorio.");
        String dniLimpio = validarDni(dni);
        RolUsuario rol = obtenerRol(rolId);

        if (usuarioRepository.existsByUsernameIgnoreCaseAndIdNot(userLimpio, id)) {
            throw new RecursoDuplicadoException("Ya existe otra cuenta con el usuario \"" + userLimpio + "\"");
        }
        if (usuarioRepository.existsByDniAndIdNot(dniLimpio, id)) {
            throw new RecursoDuplicadoException("Ya existe otra cuenta registrada con el DNI " + dniLimpio);
        }
        if (rawPassword != null && !rawPassword.isBlank()) {
            if (rawPassword.trim().length() < 4) {
                throw new DatosInvalidosException("La nueva contraseña debe tener al menos 4 caracteres.");
            }
            cuenta.setPasswordHash(passwordHasher.hash(rawPassword.trim()));
        }

        cuenta.setUsername(userLimpio);
        cuenta.setNombre(nombreLimpio);
        cuenta.setApellido(apellidoLimpio);
        cuenta.setDni(dniLimpio);
        cuenta.setRol(rol);
        return usuarioRepository.save(cuenta);
    }

    @Transactional
    public void eliminarUsuario(Long id, Long usuarioActualId) {
        UsuarioCuenta cuenta = obtenerUsuario(id);
        if (usuarioActualId != null && usuarioActualId.equals(cuenta.getId())) {
            throw new ReglaNegocioException("No podés eliminar tu propia cuenta con la sesión activa.");
        }
        usuarioRepository.delete(cuenta);
    }

    @Transactional(readOnly = true)
    public List<RolUsuario> listarRoles() {
        return rolRepository.findAllByOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public RolUsuario obtenerRol(Long id) {
        if (id == null) {
            throw new DatosInvalidosException("Seleccioná un rol válido.");
        }
        return rolRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El rol seleccionado no existe."));
    }

    @Transactional(readOnly = true)
    public long cantidadUsuariosPorRol(Long rolId) {
        return usuarioRepository.countByRolId(rolId);
    }

    @Transactional
    public RolUsuario crearRol(String nombre, boolean soloTurnosPropios, Set<Permiso> permisos) {
        String nombreLimpio = limpiarTexto(nombre, "Ingresá un nombre para el rol.");
        if (rolRepository.existsByNombreIgnoreCase(nombreLimpio)) {
            throw new RecursoDuplicadoException("Ya existe un rol llamado \"" + nombreLimpio + "\".");
        }
        RolUsuario rol = new RolUsuario(
                nombreLimpio,
                soloTurnosPropios,
                false,
                permisos == null ? EnumSet.noneOf(Permiso.class) : permisos);
        return rolRepository.save(rol);
    }

    @Transactional
    public RolUsuario actualizarRol(Long id, String nombre, boolean soloTurnosPropios, Set<Permiso> permisos) {
        RolUsuario rol = obtenerRol(id);
        if ("ADMIN".equalsIgnoreCase(rol.getNombre())) {
            rol.setPermisos(EnumSet.allOf(Permiso.class));
            rol.setSoloTurnosPropios(false);
            return rolRepository.save(rol);
        }
        String nombreLimpio = limpiarTexto(nombre, "Ingresá un nombre para el rol.");
        if (!rol.isSistema()) {
            if (rolRepository.existsByNombreIgnoreCaseAndIdNot(nombreLimpio, id)) {
                throw new RecursoDuplicadoException("Ya existe otro rol llamado \"" + nombreLimpio + "\".");
            }
            rol.setNombre(nombreLimpio);
        }
        rol.setSoloTurnosPropios(soloTurnosPropios);
        rol.setPermisos(permisos == null ? EnumSet.noneOf(Permiso.class) : permisos);
        return rolRepository.save(rol);
    }

    @Transactional
    public void eliminarRol(Long id) {
        RolUsuario rol = obtenerRol(id);
        if (rol.isSistema()) {
            throw new ReglaNegocioException("El rol \"" + rol.getNombre() + "\" es un rol base del sistema y no se puede eliminar.");
        }
        long enUso = usuarioRepository.countByRolId(id);
        if (enUso > 0) {
            throw new ReglaNegocioException("No se puede eliminar el rol \"" + rol.getNombre()
                    + "\" porque está asignado a " + enUso + (enUso == 1 ? " cuenta." : " cuentas."));
        }
        rolRepository.delete(rol);
    }

    private String limpiarTexto(String valor, String mensajeError) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new DatosInvalidosException(mensajeError);
        }
        return valor.trim();
    }

    private String validarDni(String dni) {
        String limpio = limpiarTexto(dni, "El DNI es obligatorio.");
        if (!limpio.matches("\\d{7,8}")) {
            throw new DatosInvalidosException("El DNI debe contener 7 u 8 dígitos numéricos.");
        }
        return limpio;
    }
}

