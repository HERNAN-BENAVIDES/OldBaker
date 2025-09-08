package co.edu.uniquindio.oldbaker.services;

import co.edu.uniquindio.oldbaker.model.Usuario;
import co.edu.uniquindio.oldbaker.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


/**
 * Servicio para gestionar operaciones relacionadas con la entidad Usuario.
 * Proporciona métodos para buscar usuarios por su email y estado activo.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    // Método para encontrar un usuario activo por su email
    public Usuario findByEmailAndActivoTrue(String userEmail) {
        return usuarioRepository.findByEmailAndActivoTrue(userEmail).orElse(null);
    }

    // Método para encontrar un usuario por su email y verificar que esté activo
    public Usuario findByEmailAndActivo(String username) {
        return usuarioRepository.findByEmailAndActivoTrue(username).orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

    }
}