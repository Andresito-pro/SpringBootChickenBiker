package com.chickenbikerv2.repository;

import com.chickenbikerv2.entity.Usuarios;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UsuariosRepository extends JpaRepository<Usuarios, Integer> {

    Optional<Usuarios> findByCorreoUsuario(String correoUsuario);
    Optional<Usuarios> findByDocumentoUsuario(String documentoUsuario);
    boolean existsByCorreoUsuario(String correoUsuario);
    boolean existsByDocumentoUsuario(String documentoUsuario);
}