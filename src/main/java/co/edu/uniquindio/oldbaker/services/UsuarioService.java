package co.edu.uniquindio.oldbaker.services;

import co.edu.uniquindio.oldbaker.dto.DireccionResponseDTO;
import co.edu.uniquindio.oldbaker.model.Direccion;
import co.edu.uniquindio.oldbaker.model.Usuario;
import co.edu.uniquindio.oldbaker.repositories.DireccionRepository;
import co.edu.uniquindio.oldbaker.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * Servicio para gestionar operaciones relacionadas con la entidad Usuario.
 * Proporciona métodos para buscar usuarios por su email y estado activo.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final DireccionRepository direccionRepository;

    // Método para encontrar un usuario activo por su email
    public Usuario findByEmailAndActivoTrue(String userEmail) {
        return usuarioRepository.findByEmailAndActivoTrue(userEmail).orElse(null);
    }

    // Método para encontrar un usuario por su email y verificar que esté activo
    public Usuario findByEmailAndActivo(String username) {
        return usuarioRepository.findByEmailAndActivoTrue(username).orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

    }

    /**
     * Marca un usuario existente como inactivo (baja) en la base de datos.
     * Retorna true si se actualizó algún registro, false si no existe el usuario.
     */
    @Transactional
    public boolean deactivateUser(Long id) {
        int updated = usuarioRepository.deactivateById(id);
        if (updated > 0) {
            log.info("Usuario id={} desactivado", id);
            return true;
        } else {
            log.warn("Intento de desactivar usuario id={} pero no se encontró", id);
            return false;
        }
    }

    public List<DireccionResponseDTO> obtenerDireccionUsuario(Long idUsuario) {

        var usuarioOpt = usuarioRepository.findById(idUsuario);

        if (usuarioOpt.isEmpty()){
            return null;
        }
        List<Direccion> dir = direccionRepository.obtenerDireccionUsuario(usuarioOpt.get());

        return parseDireccion(dir);
    }

    private List<DireccionResponseDTO> parseDireccion(List<Direccion> dir) {
        List<DireccionResponseDTO> direccionResponseDTO = new ArrayList<>();
        if (!dir.isEmpty()) {

            dir.forEach(d -> {
                DireccionResponseDTO direccionDTO = new DireccionResponseDTO();
                direccionDTO.setId(d.getId());
                direccionDTO.setCiudad(d.getCiudad());
                direccionDTO.setCalle(d.getCalle());
                direccionDTO.setCarrera(d.getCarrera());
                direccionDTO.setNumero(d.getNumero());
                direccionDTO.setBarrio(d.getBarrio());
                direccionDTO.setNumeroTelefono(d.getNumeroTelefono());
                direccionResponseDTO.add(direccionDTO);
            });
        }
        return direccionResponseDTO;
    }

    public DireccionResponseDTO agregarDireccionUsuario(Long idUsuario, DireccionResponseDTO direccionDTO) {
        var usuarioOpt = usuarioRepository.findById(idUsuario);

        if (usuarioOpt.isEmpty()){
            return null;
        }

        Direccion direccion = new Direccion();
        direccion.setCiudad(direccionDTO.getCiudad());
        direccion.setCalle(direccionDTO.getCalle());
        direccion.setCarrera(direccionDTO.getCarrera());
        direccion.setNumero(direccionDTO.getNumero());
        direccion.setBarrio(direccionDTO.getBarrio());
        direccion.setNumeroTelefono(direccionDTO.getNumeroTelefono());
        direccion.setUsuario(usuarioOpt.get());

        Direccion direccionGuardada = direccionRepository.save(direccion);

        DireccionResponseDTO direccionResponseDTO = new DireccionResponseDTO();
        direccionResponseDTO.setId(direccionGuardada.getId());
        direccionResponseDTO.setCiudad(direccionGuardada.getCiudad());
        direccionResponseDTO.setCalle(direccionGuardada.getCalle());
        direccionResponseDTO.setCarrera(direccionGuardada.getCarrera());
        direccionResponseDTO.setNumero(direccionGuardada.getNumero());
        direccionResponseDTO.setBarrio(direccionGuardada.getBarrio());
        direccionResponseDTO.setNumeroTelefono(direccionGuardada.getNumeroTelefono());

        return direccionResponseDTO;
    }
}