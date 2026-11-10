package com.chickenbikerv2.service;

import com.chickenbikerv2.entity.Mesas;
import com.chickenbikerv2.repository.MesasRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MesasService {

    private final MesasRepository mesasRepository;

    public MesasService(MesasRepository mesasRepository) {
        this.mesasRepository = mesasRepository;
    }

    public List<Mesas> listar() {
        return mesasRepository.findAll();
    }

    public Optional<Mesas> buscarPorId(Integer id) {
        return mesasRepository.findById(id);
    }

    public Optional<Mesas> buscarPorNumero(Integer numeroMesa) {
        return mesasRepository.findByNumeroMesa(numeroMesa);
    }

    public Mesas guardar(Mesas mesas) {
        return mesasRepository.save(mesas);
    }

    public void eliminar(Integer id) {
        mesasRepository.deleteById(id);
    }
}
