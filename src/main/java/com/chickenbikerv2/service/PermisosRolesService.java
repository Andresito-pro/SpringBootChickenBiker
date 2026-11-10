package com.chickenbikerv2.service;

import com.chickenbikerv2.entity.PermisosRoles;
import com.chickenbikerv2.repository.PermisosRolesRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PermisosRolesService {

    private final PermisosRolesRepository permisosRolesRepository;

    public PermisosRolesService(PermisosRolesRepository permisosRolesRepository) {
        this.permisosRolesRepository = permisosRolesRepository;
    }

    public List<PermisosRoles> listar() {
        return permisosRolesRepository.findAll();
    }

    public Optional<PermisosRoles> buscarPorId(PermisosRoles.PermisosRolesId id) {
        return permisosRolesRepository.findById(id);
    }

    public PermisosRoles guardar(PermisosRoles permisosRoles) {
        return permisosRolesRepository.save(permisosRoles);
    }

    public void eliminar(PermisosRoles.PermisosRolesId id) {
        permisosRolesRepository.deleteById(id);
    }
}