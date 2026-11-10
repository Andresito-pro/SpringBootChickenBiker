package com.chickenbikerv2.service;

import com.chickenbikerv2.entity.Roles;
import com.chickenbikerv2.repository.RolesRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RolesService {

    private final RolesRepository rolesRepository;
    public RolesService(RolesRepository rolesRepository) {
        this.rolesRepository = rolesRepository;
    }

    public List<Roles> listar(){
        return rolesRepository.findAll();
    }

    public Roles guardar(Roles roles){
        return rolesRepository.save(roles);
    }

}


