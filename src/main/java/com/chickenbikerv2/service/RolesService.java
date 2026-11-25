package com.chickenbikerv2.service;

import com.chickenbikerv2.entity.Roles;
import com.chickenbikerv2.repository.RolesRepository;
import com.chickenbikerv2.security.DashboardPorRol;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RolesService {

    private final RolesRepository rolesRepository;

    public RolesService(RolesRepository rolesRepository) {
        this.rolesRepository = rolesRepository;
    }

    public List<Roles> listar() {
        return rolesRepository.findAll();
    }

    public List<Roles> listarRoles() {
        return listar();
    }

    public Optional<Roles> buscarPorId(Integer id) {
        return rolesRepository.findById(id);
    }

    public Roles guardar(Roles roles) {
        return rolesRepository.save(roles);
    }

    public void insertar(Roles rol) {
        guardar(rol);
    }

    public void eliminar(Integer id) {
        rolesRepository.deleteById(id);
    }

    /**
     * Reglas que evitan duplicar roles y escalar privilegios desde el módulo Roles
     * (accesible con el permiso "Gestionar roles", que puede tener un rol no administrador):
     *  - No se puede crear ni renombrar un rol con el nombre de otro existente.
     *  - El nombre "Administrador" está reservado: concede ROLE_Administrador.
     *  - El rol Administrador no se renombra ni se elimina.
     *  - Nadie edita ni elimina su propio rol.
     * Lanza IllegalStateException con el motivo.
     */
    public void validarGuardado(Roles rol, int idRolActor) {
        String nombre = rol.getNombreRol() == null ? "" : rol.getNombreRol().trim();
        boolean esEdicion = rol.getIdRoles() != null && rol.getIdRoles() > 0;

        if (esEdicion) {
            Roles actual = rolesRepository.findById(rol.getIdRoles())
                    .orElseThrow(() -> new IllegalStateException("El rol seleccionado no existe."));
            if (actual.getIdRoles() == idRolActor) {
                throw new IllegalStateException("No puedes modificar tu propio rol.");
            }
            if (esAdministrador(actual.getNombreRol()) && !esAdministrador(nombre)) {
                throw new IllegalStateException("El rol Administrador está protegido y no se puede renombrar.");
            }
            if (!esAdministrador(actual.getNombreRol()) && esAdministrador(nombre)) {
                throw new IllegalStateException("El nombre \"Administrador\" está reservado.");
            }
        } else if (esAdministrador(nombre)) {
            throw new IllegalStateException("El nombre \"Administrador\" está reservado.");
        }

        boolean duplicado = rolesRepository.findAll().stream()
                .anyMatch(r -> r.getNombreRol() != null && r.getNombreRol().trim().equalsIgnoreCase(nombre)
                        && (!esEdicion || !r.getIdRoles().equals(rol.getIdRoles())));
        if (duplicado) {
            throw new IllegalStateException("Ya existe un rol llamado \"" + nombre + "\".");
        }
    }

    /** No se elimina el rol propio ni el rol Administrador. */
    public void validarEliminacion(Integer idRol, int idRolActor) {
        Roles rol = rolesRepository.findById(idRol)
                .orElseThrow(() -> new IllegalStateException("El rol seleccionado no existe."));
        if (rol.getIdRoles() == idRolActor) {
            throw new IllegalStateException("No puedes eliminar tu propio rol.");
        }
        if (esAdministrador(rol.getNombreRol())) {
            throw new IllegalStateException("El rol Administrador está protegido y no se puede eliminar.");
        }
    }

    private static boolean esAdministrador(String nombre) {
        return nombre != null && DashboardPorRol.ROL_ADMINISTRADOR.equalsIgnoreCase(nombre.trim());
    }
}
