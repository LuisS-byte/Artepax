package com.catolica.Artepax.documento.service;

import com.catolica.Artepax.documento.dto.DocumentoResponseDTO;
import com.catolica.Artepax.documento.dto.PlantillaResponseDTO;
import com.catolica.Artepax.documento.dto.PlantillaUpdateDTO;
import com.catolica.Artepax.documento.entity.DocumentoTipo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
public interface DocumentoService {

    DocumentoResponseDTO generarDocumento(Long idEvento, DocumentoTipo tipoDocumento);

    DocumentoResponseDTO reenviarDocumento(Long idDocumento);

    List<DocumentoResponseDTO> listarPorEvento(Long idEvento);

    DocumentoResponseDTO obtenerDocumento(Long idDocumento);

    Page<DocumentoResponseDTO> obtenerTodos(Pageable pageable);

    List<PlantillaResponseDTO> listarPlantillas();

    PlantillaResponseDTO obtenerPlantilla(Long idPlantilla);

    PlantillaResponseDTO actualizarPlantilla(Long idPlantilla, PlantillaUpdateDTO dto);
}
