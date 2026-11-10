package com.chickenbikerv2.repository;

import com.chickenbikerv2.entity.TiposDocumentos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TiposDocumentosRepository extends JpaRepository<TiposDocumentos, Integer> {
}