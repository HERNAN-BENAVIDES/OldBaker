package co.edu.uniquindio.oldbaker.services;

import co.edu.uniquindio.oldbaker.model.Usuario;
import co.edu.uniquindio.oldbaker.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;


    public Usuario findByEmailAndActivoTrue(String userEmail) {
        return usuarioRepository.findByEmailAndActivoTrue(userEmail).orElse(null);
    }

    public Usuario findByEmailAndActivo(String username) {
        return usuarioRepository.findByEmailAndActivoTrue(username).orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

    }
}