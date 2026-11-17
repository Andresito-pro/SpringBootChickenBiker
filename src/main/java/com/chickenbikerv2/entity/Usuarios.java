package com.chickenbikerv2.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Entity
@Table(name = "usuarios")
@Getter @Setter
public class Usuarios {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private int idUsuario;

    @Column(name = "nombre_usuario")
    private String nombreUsuario;

    @Column(name = "apellido_usuario")
    private String apellidoUsuario;

    @Column(name = "documento_usuario")
    private String documentoUsuario;

    @Column(name = "correo_usuario")
    private String correoUsuario;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "password_usuario")
    private String passwordUsuario;

    @Column(name = "telefono_usuario")
    private String telefonoUsuario;

    // Formato ISO (yyyy-MM-dd) que envía <input type="date">.
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "direccion_usuario")
    private String direccionUsuario;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Column(name = "autorizacion_datos")
    private Byte autorizacionDatos;

    @Column(name = "roles_id_roles")
    private int rolesIdRoles;

    @Column(name = "tipo_documento_id_tipo_documento")
    private int tipoDocumentoIdTipoDocumento;

    @Column(name = "estados_usuarios_id_estados_usuarios")
    private int estadosUsuariosIdEstadosUsuarios;
}