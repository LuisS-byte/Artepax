package com.catolica.Artepax.authentication.user.controller;

import com.catolica.Artepax.authentication.user.dto.UsuarioCreateRequest;
import com.catolica.Artepax.authentication.user.dto.UsuarioEstadoRequest;
import com.catolica.Artepax.authentication.user.dto.UsuarioResponseDTO;
import com.catolica.Artepax.authentication.user.entity.Rol;
import com.catolica.Artepax.authentication.user.entity.Usuario;
import com.catolica.Artepax.authentication.user.service.UsuarioService;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.Map;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Standalone {@link MockMvc} tests for {@link UsuarioController}.
 * <p>
 * Uses Mockito + standalone MockMvc setup instead of {@code @WebMvcTest}
 * because the project's security filter chain (JWT, CORS, in-memory
 * {@code UserDetailsManager}) produces bean conflicts under the web slice.
 * This approach is faster and tests the same routing + validation behavior.
 * <p>
 * Security (e.g. {@code @PreAuthorize}) is not evaluated in standalone mode;
 * endpoint authorization is verified by the controller's annotation presence.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioController standalone MockMvc")
class UsuarioControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private UsuarioController controller;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setValidator(new LocalValidatorFactoryBean())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private UsuarioCreateRequest buildValidRequest() {
        return new UsuarioCreateRequest("Test User", "test@artepax.com",
                "password123", "555-0100", 1);
    }

    private UsuarioResponseDTO buildResponse(Long id, String nombre, String correo,
                                              Boolean estado, String rolNombre) {
        return new UsuarioResponseDTO(id, nombre, correo, "555-0100",
                "LOCAL", estado, 1, rolNombre, LocalDateTime.now());
    }

    // -------------------------------------------------------------------------
    // POST /api/usuarios — 201 Created (happy path)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/usuarios with valid body returns 201 Created")
    void crearUsuario_deberiaRetornar201() throws Exception {
        UsuarioCreateRequest request = buildValidRequest();
        UsuarioResponseDTO response = buildResponse(1L, "Test User",
                "test@artepax.com", true, "ADMIN");

        when(usuarioService.crearUsuarioLocal(any(UsuarioCreateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Test User"))
                .andExpect(jsonPath("$.correo").value("test@artepax.com"))
                .andExpect(jsonPath("$.estado").value(true));
    }

    // -------------------------------------------------------------------------
    // POST /api/usuarios — 400 Bad Request (invalid body)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/usuarios with empty body returns 400 Bad Request")
    void crearUsuario_conDatosInvalidos_deberiaRetornar400() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/usuarios with missing password returns 400")
    void crearUsuario_sinPassword_retorna400() throws Exception {
        String json = """
                {
                    "nombre": "Test",
                    "correo": "test@test.com",
                    "rolId": 1
                }
                """;
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // GET /api/usuarios — 200 OK
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/usuarios returns 200 with user list")
    void listarUsuarios_deberiaRetornar200() throws Exception {
        List<UsuarioResponseDTO> usuarios = List.of(
                buildResponse(1L, "User One", "u1@artepax.com", true, "ADMIN"),
                buildResponse(2L, "User Two", "u2@artepax.com", false, "USER")
        );

        when(usuarioService.listarUsuarios()).thenReturn(usuarios);

        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].correo").value("u1@artepax.com"))
                .andExpect(jsonPath("$[1].correo").value("u2@artepax.com"));
    }

    // -------------------------------------------------------------------------
    // PATCH /api/usuarios/{id}/estado — 200 OK
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("PATCH /api/usuarios/{id}/estado returns 200 with updated user")
    void cambiarEstado_deberiaRetornar200() throws Exception {
        UsuarioEstadoRequest request = new UsuarioEstadoRequest(false);
        UsuarioResponseDTO response = buildResponse(2L, "Target User",
                "target@artepax.com", false, "ADMIN");

        when(usuarioService.cambiarEstado(eq(2L), any(UsuarioEstadoRequest.class),
                nullable(Usuario.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/api/usuarios/2/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.estado").value(false));
    }

    // -------------------------------------------------------------------------
    // PATCH /api/usuarios/{id}/estado — 400 (invalid body)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("PATCH /api/usuarios/{id}/estado with missing activo returns 400")
    void cambiarEstado_conDatosInvalidos_deberiaRetornar400() throws Exception {
        mockMvc.perform(patch("/api/usuarios/1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // GET /api/usuarios/me — 200 OK (preserved endpoint)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/usuarios/me returns current user info (direct controller call)")
    void getMe_deberiaRetornar200() {
        Rol rol = Rol.builder().idRol(1).nombre("ADMIN").build();
        Usuario usuario = Usuario.builder()
                .idUsuario(1L)
                .nombre("Current User")
                .correo("current@artepax.com")
                .telefono("555-0100")
                .rol(rol)
                .build();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        usuario, null, usuario.getAuthorities());

        ResponseEntity<Map<String, Object>> response = controller.obtenerUsuarioActual(auth);

        assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().get("id"));
        assertEquals("Current User", response.getBody().get("nombre"));
        assertEquals("current@artepax.com", response.getBody().get("correo"));
        assertEquals("555-0100", response.getBody().get("telefono"));
        assertEquals("ADMIN", response.getBody().get("rol"));
    }

    // -------------------------------------------------------------------------
    // GET /api/usuarios/me — 401 without auth
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/usuarios/me returns 401 when unauthenticated")
    void getMe_sinAuth_retorna401() throws Exception {
        mockMvc.perform(get("/api/usuarios/me"))
                .andExpect(status().isUnauthorized());
    }
}
