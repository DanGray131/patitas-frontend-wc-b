package pe.edu.cibertec.patitas_frontend_wc.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.edu.cibertec.patitas_frontend_wc.dto.LoginRequestDTO;
import pe.edu.cibertec.patitas_frontend_wc.dto.LoginResponseDTO;
import pe.edu.cibertec.patitas_frontend_wc.dto.LogoutRequestDTO;
import pe.edu.cibertec.patitas_frontend_wc.dto.LogoutResponseDTO;
import pe.edu.cibertec.patitas_frontend_wc.viewmodel.LoginModel;
import reactor.core.publisher.Mono;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/login")
public class LoginController {

    @Autowired
    WebClient webClientAutenticacion;

    @GetMapping("/inicio")
    public String mostrarFormularioLogin(Model model) {
        model.addAttribute("loginRequest", new LoginRequestDTO("", "", ""));

        if (!model.containsAttribute("loginModel")) {
            model.addAttribute("loginModel", new LoginModel("", "", ""));
        }
        return "inicio";
    }

    @PostMapping("/autenticar")
    public String autenticar(@RequestParam("tipoDocumento") String tipoDocumento,
                             @RequestParam("numeroDocumento") String numeroDocumento,
                             @RequestParam("password") String password,
                             Model model) {

        // Validar campos de entrada
        if (tipoDocumento == null || tipoDocumento.trim().isEmpty() ||
                numeroDocumento == null || numeroDocumento.trim().isEmpty() ||
                password == null || password.trim().isEmpty()) {

            LoginModel loginModel = new LoginModel("01", "Error: Debe completar correctamente sus credenciales", "");
            model.addAttribute("loginModel", loginModel);
            return "inicio";
        }

        try {
            // Invocar API de validación de usuario
            LoginRequestDTO loginRequestDTO = new LoginRequestDTO(tipoDocumento, numeroDocumento, password);

            Mono<LoginResponseDTO> monoLoginResponseDTO = webClientAutenticacion.post()
                    .uri("/login")
                    .body(Mono.just(loginRequestDTO), LoginRequestDTO.class)
                    .retrieve()
                    .bodyToMono(LoginResponseDTO.class);

            // Recuperar resultado del Mono (bloqueante)
            LoginResponseDTO loginResponseDTO = monoLoginResponseDTO.block();

            // Validar respuesta
            if (loginResponseDTO.codigo().equals("00")) {
                LoginModel loginModel = new LoginModel("00", "", loginResponseDTO.nombreUsuario());
                model.addAttribute("loginModel", loginModel);
                model.addAttribute("loginRequestDTO1", loginRequestDTO);
                return "principal";
            } else {
                LoginModel loginModel = new LoginModel("02", "Error: Autenticación fallida", "");
                model.addAttribute("loginModel", loginModel);
                return "inicio";
            }

        } catch (Exception e) {
            LoginModel loginModel = new LoginModel("99", "Error: Ocurrió un problema en la autenticación", "");
            model.addAttribute("loginModel", loginModel);
            System.out.println(e.getMessage());
            return "inicio";
        }
    }

    @GetMapping("/principal")
    public String mostrarPaginaPrincipal(Model model) {
        if (!model.containsAttribute("loginModel")) {
            return "redirect:/login/inicio";
        }
        model.addAttribute("logoutRequest", new LogoutRequestDTO("", ""));
        return "principal";
    }

    @PostMapping("/cerrar-sesion")
    public String cerrarSesion(@RequestParam("tipoDocumento") String tipoDocumento,
                               @RequestParam("numeroDocumento") String numeroDocumento,
                               Model model) {
        try {
            LogoutRequestDTO logoutRequestDTO = new LogoutRequestDTO(tipoDocumento, numeroDocumento);

            Mono<Void> monoResponse = webClientAutenticacion.post()
                    .uri("/logout")
                    .body(Mono.just(logoutRequestDTO), LogoutRequestDTO.class)
                    .retrieve()
                    .bodyToMono(Void.class);

            // Bloquear para esperar la respuesta
            monoResponse.block();

            model.addAttribute("mensaje", "Cierre de sesión exitoso");
            return "redirect:/login/inicio";

        } catch (Exception e) {
            model.addAttribute("mensaje", "Error: Ocurrió un problema al cerrar sesión");
            System.out.println(e.getMessage());
            return "principal";
        }
    }

}
