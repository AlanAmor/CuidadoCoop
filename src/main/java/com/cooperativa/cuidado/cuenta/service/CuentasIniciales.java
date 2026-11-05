package com.cooperativa.cuidado.cuenta.service;

import com.cooperativa.cuidado.cuenta.Permiso;
import com.cooperativa.cuidado.cuenta.RolUsuario;
import com.cooperativa.cuidado.cuenta.RolUsuarioRepository;
import com.cooperativa.cuidado.cuenta.UsuarioCuenta;
import com.cooperativa.cuidado.cuenta.UsuarioCuentaRepository;
import com.cooperativa.cuidado.socio.Especialidad;
import com.cooperativa.cuidado.socio.EstadoSocio;
import com.cooperativa.cuidado.socio.Socio;
import com.cooperativa.cuidado.socio.SocioRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;

/** Crea los roles base (ADMIN, AYUDANTE, SOCIO) y las cuentas de ejemplo para prueba. */
@Component
public class CuentasIniciales implements ApplicationRunner {

    private final RolUsuarioRepository rolRepository;
    private final UsuarioCuentaRepository usuarioRepository;
    private final SocioRepository socioRepository;
    private final PasswordHasher passwordHasher;

    public CuentasIniciales(
            RolUsuarioRepository rolRepository,
            UsuarioCuentaRepository usuarioRepository,
            SocioRepository socioRepository,
            PasswordHasher passwordHasher) {
        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
        this.socioRepository = socioRepository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        RolUsuario rolAdmin = asegurarRol(
                "ADMIN",
                false,
                true,
                EnumSet.allOf(Permiso.class));

        RolUsuario rolAyudante = asegurarRol(
                "AYUDANTE",
                false,
                true,
                EnumSet.of(
                        Permiso.VER_CALENDARIO_GLOBAL,
                        Permiso.ASIGNAR_TURNOS,
                        Permiso.MODIFICAR_TURNOS,
                        Permiso.ELIMINAR_TURNOS,
                        Permiso.AGREGAR_CLIENTES,
                        Permiso.AGREGAR_SOCIOS,
                        Permiso.AGREGAR_LEGAJOS,
                        Permiso.REALIZAR_PRESUPUESTOS));

        RolUsuario rolSocio = asegurarRol(
                "SOCIO",
                true,
                true,
                EnumSet.noneOf(Permiso.class));

        asegurarUsuario("admin", "admin123", "Laura", "Administradora", "10000001", rolAdmin);
        asegurarUsuario("ayudante", "ayudante123", "Martín", "Ayudante", "10000002", rolAyudante);

        // Socio de ejemplo vinculado por DNI (usa 20900001 que coincide con Ana Gómez de los datos demo, o la crea si no existe)
        String dniSocioDemo = "20900001";
        Socio socioVinculado = socioRepository.findByDni(dniSocioDemo).orElseGet(() -> {
            Socio s = new Socio();
            s.setNombre("Ana");
            s.setApellido("Gómez");
            s.setDni(dniSocioDemo);
            s.setDireccion("Av. San Martín 101");
            s.setTelefono("1140000001");
            s.setCorreo("ana.gomez.demo@example.com");
            s.setCbu("2850590900000000000001");
            s.setEstado(EstadoSocio.ACTIVO_EN_SERVICIO);
            s.setEspecialidad(Especialidad.CUIDADOR);
            return socioRepository.save(s);
        });
        asegurarUsuario("socio", "socio123", socioVinculado.getNombre(), socioVinculado.getApellido(), dniSocioDemo, rolSocio);
    }

    private RolUsuario asegurarRol(String nombre, boolean soloTurnosPropios, boolean sistema, Set<Permiso> permisos) {
        return rolRepository.findByNombreIgnoreCase(nombre).orElseGet(() ->
                rolRepository.save(new RolUsuario(nombre, soloTurnosPropios, sistema, permisos)));
    }

    private void asegurarUsuario(
            String username,
            String rawPassword,
            String nombre,
            String apellido,
            String dni,
            RolUsuario rol) {
        if (usuarioRepository.existsByUsernameIgnoreCase(username) || usuarioRepository.existsByDni(dni)) {
            return;
        }
        UsuarioCuenta cuenta = new UsuarioCuenta();
        cuenta.setUsername(username);
        cuenta.setPasswordHash(passwordHasher.hash(rawPassword));
        cuenta.setNombre(nombre);
        cuenta.setApellido(apellido);
        cuenta.setDni(dni);
        cuenta.setRol(rol);
        usuarioRepository.save(cuenta);
    }
}

