package com.chickenbikerv2.service;

import com.chickenbikerv2.entity.Permisos;
import com.chickenbikerv2.entity.PermisosRoles;
import com.chickenbikerv2.entity.Roles;
import com.chickenbikerv2.repository.PermisosRepository;
import com.chickenbikerv2.repository.PermisosRolesRepository;
import com.chickenbikerv2.repository.RolesRepository;
import com.chickenbikerv2.security.DashboardPorRol;
import com.chickenbikerv2.security.PermisosSistema;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GestionPermisosService {

    public record PermisoVista(int idPermiso, String nombre, String modulo, String descripcion,
                               boolean aplicado, boolean asignado) {
    }

    public record RolVista(int idRol, String nombre, String descripcion, long totalPermisos,
                           boolean protegido, String motivoProteccion) {
    }

    public record ResultadoCambio(String nombreRol, List<String> asignados, List<String> revocados) {
        public boolean sinCambios() {
            return asignados.isEmpty() && revocados.isEmpty();
        }
    }

    private final RolesRepository rolesRepository;
    private final PermisosRepository permisosRepository;
    private final PermisosRolesRepository permisosRolesRepository;

    public GestionPermisosService(RolesRepository rolesRepository,
                                  PermisosRepository permisosRepository,
                                  PermisosRolesRepository permisosRolesRepository) {
        this.rolesRepository = rolesRepository;
        this.permisosRepository = permisosRepository;
        this.permisosRolesRepository = permisosRolesRepository;
    }

    @Transactional(readOnly = true)
    public List<RolVista> listarRoles(int idRolActor) {
        Map<Integer, Long> totales = permisosRolesRepository.findAll().stream()
                .collect(Collectors.groupingBy(PermisosRoles::getRolesIdRoles, Collectors.counting()));
        return rolesRepository.findAll().stream()
                .sorted(Comparator.comparing(Roles::getIdRoles))
                .map(r -> new RolVista(r.getIdRoles(), r.getNombreRol(), r.getDescripcionRol(),
                        totales.getOrDefault(r.getIdRoles(), 0L),
                        motivoProteccion(r, idRolActor) != null, motivoProteccion(r, idRolActor)))
                .toList();
    }

    /** Catálogo completo de permisos con la marca de los asignados al rol. */
    @Transactional(readOnly = true)
    public List<PermisoVista> permisosDelRol(int idRol) {
        Set<Integer> asignados = permisosRolesRepository.findByRolesIdRoles(idRol).stream()
                .map(PermisosRoles::getPermisosIdPermisos)
                .collect(Collectors.toSet());
        return permisosRepository.findAllByOrderByNombrePermisoAsc().stream()
                .map(p -> {
                    PermisosSistema.Descripcion d = PermisosSistema.describir(p.getNombrePermiso());
                    return new PermisoVista(p.getIdPermisos(), p.getNombrePermiso(), d.modulo(), d.descripcion(),
                            d.aplicado(), asignados.contains(p.getIdPermisos()));
                })
                .sorted(Comparator.comparing(PermisoVista::modulo).thenComparing(PermisoVista::nombre))
                .toList();
    }

    /** null si el rol se puede editar; si no, el motivo para mostrarlo en la interfaz. */
    public String motivoProteccion(Roles rol, int idRolActor) {
        if (rol.getIdRoles() != null && rol.getIdRoles() == idRolActor) {
            return "Es tu propio rol: nadie puede modificar sus propios privilegios.";
        }
        if (DashboardPorRol.ROL_ADMINISTRADOR.equalsIgnoreCase(rol.getNombreRol())) {
            return "El rol Administrador está protegido para no perder el acceso administrativo.";
        }
        return null;
    }

    /** Lanza IllegalStateException si el actor no puede modificar los permisos de ese rol. */
    public Roles verificarRolEditable(int idRol, int idRolActor) {
        Roles rol = rolesRepository.findById(idRol)
                .orElseThrow(() -> new IllegalArgumentException("El rol seleccionado no existe."));
        String motivo = motivoProteccion(rol, idRolActor);
        if (motivo != null) {
            throw new IllegalStateException(motivo);
        }
        return rol;
    }

    /**
     * Deja al rol EXACTAMENTE con los permisos indicados: asigna los nuevos y
     * revoca los que se desmarcaron, en una sola transacción.
     */
    @Transactional
    public ResultadoCambio actualizarPermisos(int idRol, Collection<Integer> idsPermisos, int idRolActor) {
        Roles rol = verificarRolEditable(idRol, idRolActor);

        Set<Integer> solicitados = idsPermisos == null ? Set.of() : new HashSet<>(idsPermisos);
        Map<Integer, Permisos> catalogo = permisosRepository.findAll().stream()
                .collect(Collectors.toMap(Permisos::getIdPermisos, Function.identity()));
        if (!catalogo.keySet().containsAll(solicitados)) {
            throw new IllegalArgumentException("Se recibió un permiso que no existe. Recarga la página e inténtalo de nuevo.");
        }

        List<PermisosRoles> actuales = permisosRolesRepository.findByRolesIdRoles(idRol);
        Set<Integer> idsActuales = actuales.stream().map(PermisosRoles::getPermisosIdPermisos).collect(Collectors.toSet());

        List<String> revocados = new ArrayList<>();
        for (PermisosRoles pr : actuales) {
            if (!solicitados.contains(pr.getPermisosIdPermisos())) {
                permisosRolesRepository.delete(pr);
                revocados.add(nombre(catalogo, pr.getPermisosIdPermisos()));
            }
        }

        List<String> asignados = new ArrayList<>();
        for (Integer idPermiso : solicitados) {
            if (!idsActuales.contains(idPermiso)) {
                PermisosRoles nuevo = new PermisosRoles();
                nuevo.setPermisosIdPermisos(idPermiso);
                nuevo.setRolesIdRoles(idRol);
                permisosRolesRepository.save(nuevo);
                asignados.add(nombre(catalogo, idPermiso));
            }
        }
        asignados.sort(String::compareTo);
        revocados.sort(String::compareTo);
        return new ResultadoCambio(rol.getNombreRol(), asignados, revocados);
    }

    private static String nombre(Map<Integer, Permisos> catalogo, int idPermiso) {
        Permisos p = catalogo.get(idPermiso);
        return p != null ? p.getNombrePermiso() : "Permiso #" + idPermiso;
    }
}
