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

    public DireccionResponseDTO obtenerDireccionUsuario(Long idUsuario) {

        var usuarioOpt = usuarioRepository.findById(idUsuario);

        if (usuarioOpt.isEmpty()){
            return null;
        }
        Direccion dir = direccionRepository.obtenerDireccionUsuario(usuarioOpt.get());

        return parseDireccion(dir);
    }

    private DireccionResponseDTO parseDireccion(Direccion dir) {
        DireccionResponseDTO direccionResponseDTO = new DireccionResponseDTO();
        if (dir != null) {
            direccionResponseDTO.setId(dir.getId());
            direccionResponseDTO.setCiudad(dir.getCiudad());
            direccionResponseDTO.setCalle(dir.getCalle());
            direccionResponseDTO.setCarrera(dir.getCarrera());
            direccionResponseDTO.setNumero(dir.getNumero());
            direccionResponseDTO.setBarrio(dir.getBarrio());
            direccionResponseDTO.setNumeroTelefono(dir.getNumeroTelefono());
        }
        return direccionResponseDTO;
    }
}