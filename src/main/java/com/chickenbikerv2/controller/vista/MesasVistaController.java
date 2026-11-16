package com.chickenbikerv2.controller.vista;

import com.chickenbikerv2.entity.EstadosMesas;
import com.chickenbikerv2.entity.Mesas;
import com.chickenbikerv2.service.EstadosMesasService;
import com.chickenbikerv2.service.MesasService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/mesas")
public class MesasVistaController {

    private final MesasService mesasService;
    private final EstadosMesasService estadosMesasService;

    public MesasVistaController(MesasService mesasService,
                                EstadosMesasService estadosMesasService) {
        this.mesasService = mesasService;
        this.estadosMesasService = estadosMesasService;
    }

    @GetMapping({"", "/listar"})
    public String listar(Model model) {
        List<Mesas> listaMesas = mesasService.listar();
        List<EstadosMesas> listaEstadosMesas = estadosMesasService.listar();

        Map<Integer, String> mapaEstados = listaEstadosMesas.stream()
                .collect(Collectors.toMap(
                        EstadosMesas::getIdEstadoMesa,
                        EstadosMesas::getNombreEstadoMesa,
                        (existente, reemplazo) -> existente
                ));

        model.addAttribute("listaMesas", listaMesas);
        model.addAttribute("totalMesas", listaMesas.size());
        model.addAttribute("listaEstadosMesas", listaEstadosMesas);
        model.addAttribute("mapaEstados", mapaEstados);
        model.addAttribute("menuActivo", "mesas");
        return "mesas/listarMesas";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("mesa", new Mesas());
        model.addAttribute("listaEstadosMesas", estadosMesasService.listar());
        model.addAttribute("menuActivo", "mesas");
        return "mesas/nuevaMesa";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) {
        Mesas mesa = mesasService.buscarPorId(id).orElse(null);
        if (mesa == null) {
            redirectAttributes.addFlashAttribute("error", "La mesa seleccionada no existe.");
            return "redirect:/mesas";
        }

        model.addAttribute("mesa", mesa);
        model.addAttribute("listaEstadosMesas", estadosMesasService.listar());
        model.addAttribute("menuActivo", "mesas");
        return "mesas/editarMesa";
    }

    @PostMapping
    public String guardar(@ModelAttribute("mesa") Mesas mesa,
                          RedirectAttributes redirectAttributes) {
        boolean esEdicion = mesa.getIdMesa() > 0;
        String rutaError = esEdicion
                ? "redirect:/mesas/editar/" + mesa.getIdMesa()
                : "redirect:/mesas/nuevo";

        if (mesa.getNumeroMesa() <= 0) {
            redirectAttributes.addFlashAttribute("error", "El número de mesa debe ser mayor a 0.");
            return rutaError;
        }

        if (mesa.getEstadoMesaIdEstadoMesa() <= 0) {
            redirectAttributes.addFlashAttribute("error", "Debes seleccionar un estado válido para la mesa.");
            return rutaError;
        }

        Optional<Mesas> existenteNumero = mesasService.buscarPorNumero(mesa.getNumeroMesa());
        if (existenteNumero.isPresent() && existenteNumero.get().getIdMesa() != mesa.getIdMesa()) {
            redirectAttributes.addFlashAttribute("error",
                    "Ya existe una mesa registrada con el número " + mesa.getNumeroMesa() + ".");
            return rutaError;
        }

        try {
            mesasService.guardar(mesa);
            redirectAttributes.addFlashAttribute("mensaje",
                    esEdicion ? "Mesa actualizada correctamente." : "Mesa registrada correctamente.");
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("error", "No se pudo guardar la mesa. Verifica que los datos sean válidos.");
            return rutaError;
        }

        return "redirect:/mesas";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            mesasService.eliminar(id);
            redirectAttributes.addFlashAttribute("mensaje", "Mesa eliminada correctamente.");
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("error", "No se puede eliminar esta mesa porque tiene pedidos o registros asociados.");
        }
        return "redirect:/mesas";
    }
}