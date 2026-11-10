package com.chickenbikerv2.service;

import com.chickenbikerv2.entity.TiposDocumentos;
import com.chickenbikerv2.repository.TiposDocumentosRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TiposDocumentosService {

    private final TiposDocumentosRepository tiposDocumentosRepository;

    public TiposDocumentosService(TiposDocumentosRepository tiposDocumentosRepository) {
        this.tiposDocumentosRepository = tiposDocumentosRepository;
    }

    public List<TiposDocumentos> listar() {
        return tiposDocumentosRepository.findAll();
    }

    public Optional<TiposDocumentos> buscarPorId(Integer id) {
        return tiposDocumentosRepository.findById(id);
    }

    public TiposDocumentos guardar(TiposDocumentos tiposDocumentos) {
        return tiposDocumentosRepository.save(tiposDocumentos);
    }

    public void eliminar(Integer id) {
        tiposDocumentosRepository.deleteById(id);
    }
}