package uce.edu.ec.api.application.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import uce.edu.ec.api.application.service.interceptor.Auditar;
import uce.edu.ec.api.domain.model.EstadoDisponibilidad;
import uce.edu.ec.api.domain.model.ReservaVehiculo;
import uce.edu.ec.api.domain.model.Usuario;
import uce.edu.ec.api.domain.model.Vehiculo;
import uce.edu.ec.api.domain.model.Vendedor;
import uce.edu.ec.api.infraestructure.repository.ReservaVehiculoRepositoryImpl;
import uce.edu.ec.api.infraestructure.repository.UsuarioRepositoryImpl;
import uce.edu.ec.api.infraestructure.repository.VehiculoRepositoryImpl;
import uce.edu.ec.api.infraestructure.repository.VendedorRepositoryImpl;

@ApplicationScoped
@Transactional
@Auditar
public class ReservaVehiculoService {

    @Inject
    private ReservaVehiculoRepositoryImpl rri;

    @Inject
    private UsuarioRepositoryImpl uri;

    @Inject
    private VendedorRepositoryImpl vri;

    @Inject
    private VehiculoRepositoryImpl vhri;

    public void crearReservaVehiculo(ReservaVehiculo reserva) {

        if (reserva == null) {
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity("El cuerpo de la petición no puede estar vacío")
                            .type(MediaType.TEXT_PLAIN)
                            .build());
        }
        if (reserva.getUsuario() == null || reserva.getUsuario().getCedula() == null) {
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity("La cédula del usuario es obligatoria")
                            .type(MediaType.TEXT_PLAIN)
                            .build());
        }
        if (reserva.getVendedor() == null || reserva.getVendedor().getCedulaVendedor() == null) {
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity("La cédula del vendedor es obligatoria")
                            .type(MediaType.TEXT_PLAIN)
                            .build());
        }
        if (reserva.getVehiculo() == null || reserva.getVehiculo().getPlaca() == null) {
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity("La placa del vehículo es obligatoria")
                            .type(MediaType.TEXT_PLAIN)
                            .build());
        }

        Usuario usuario = this.uri.find("cedula", reserva.getUsuario().getCedula()).firstResult();
        if (usuario == null) {
            throw new WebApplicationException(
                    Response.status(Response.Status.NOT_FOUND)
                            .entity("No existe un usuario registrado con la cédula " + reserva.getUsuario().getCedula())
                            .type(MediaType.TEXT_PLAIN)
                            .build());
        }

        Vendedor vendedor = this.vri.find("cedulaVendedor", reserva.getVendedor().getCedulaVendedor()).firstResult();
        if (vendedor == null) {
            throw new WebApplicationException(
                    Response.status(Response.Status.NOT_FOUND)
                            .entity("No existe un vendedor registrado con la cédula "
                                    + reserva.getVendedor().getCedulaVendedor())
                            .type(MediaType.TEXT_PLAIN)
                            .build());
        }

        Vehiculo vehiculo = this.vhri.find("placa", reserva.getVehiculo().getPlaca()).firstResult();
        if (vehiculo == null) {
            throw new WebApplicationException(
                    Response.status(Response.Status.NOT_FOUND)
                            .entity("No existe un vehículo registrado con la placa " + reserva.getVehiculo().getPlaca())
                            .type(MediaType.TEXT_PLAIN)
                            .build());
        }

        if (vehiculo.getEstadoDisponibilidad() != EstadoDisponibilidad.DISPONIBLE) {
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity("El vehículo con placa " + vehiculo.getPlaca() + " no está disponible")
                            .type(MediaType.TEXT_PLAIN)
                            .build());
        }

        if (reserva.getFechaInicio() == null || reserva.getFechaFin() == null
                || !reserva.getFechaFin().isAfter(reserva.getFechaInicio())) {
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity("La fechaFin debe ser posterior a la fechaInicio")
                            .type(MediaType.TEXT_PLAIN)
                            .build());
        }

        long dias = ChronoUnit.DAYS.between(reserva.getFechaInicio(), reserva.getFechaFin());
        dias = (dias <= 0) ? 1 : dias;

        reserva.setUsuario(usuario);
        reserva.setVendedor(vendedor);
        reserva.setVehiculo(vehiculo);
        reserva.setFechaReserva(reserva.getFechaReserva() == null ? LocalDate.now() : reserva.getFechaReserva());
        reserva.setEstado(EstadoDisponibilidad.CONFIRMADA);

        this.rri.persist(reserva);
        vehiculo.setEstadoDisponibilidad(EstadoDisponibilidad.RESERVADO);
    }

    public void crearReservasVehiculo(List<ReservaVehiculo> reservas) {

        if (reservas == null || reservas.isEmpty()) {
            throw new WebApplicationException("La lista de reservas no puede estar vacía", 400);
        }

        for (ReservaVehiculo reserva : reservas) {
            crearReservaVehiculo(reserva);
        }
    }

    public List<ReservaVehiculo> buscarTodos() {
        return this.rri.findAll().list();
    }

    public void actualizarReservaVehiculo(Integer id, ReservaVehiculo reserva) {

        ReservaVehiculo reservaExistente = this.rri.findById(id);
        if (reservaExistente == null) {
            throw new WebApplicationException("No existe la reserva con ID: " + id, 404);
        }

        if (reserva.getUsuario() != null && reserva.getUsuario().getCedula() != null) {
            Usuario nuevoUsuario = this.uri.find("cedula", reserva.getUsuario().getCedula()).firstResult();
            if (nuevoUsuario == null) {
                throw new WebApplicationException(
                        "No existe un usuario con la cédula " + reserva.getUsuario().getCedula(), 404);
            }
            reservaExistente.setUsuario(nuevoUsuario);
        }

        if (reserva.getVendedor() != null && reserva.getVendedor().getCedulaVendedor() != null) {
            Vendedor nuevoVendedor = this.vri.find("cedulaVendedor", reserva.getVendedor().getCedulaVendedor())
                    .firstResult();
            if (nuevoVendedor == null) {
                throw new WebApplicationException(
                        "No existe un vendedor con la cédula " + reserva.getVendedor().getCedulaVendedor(), 404);
            }
            reservaExistente.setVendedor(nuevoVendedor);
        }

        if (reserva.getVehiculo() != null && reserva.getVehiculo().getPlaca() != null) {

            if (!reservaExistente.getVehiculo().getPlaca().equals(reserva.getVehiculo().getPlaca())) {

                Vehiculo nuevoVehiculo = this.vhri.find("placa", reserva.getVehiculo().getPlaca()).firstResult();
                if (nuevoVehiculo == null) {
                    throw new WebApplicationException(
                            "No existe un vehículo con la placa " + reserva.getVehiculo().getPlaca(), 404);
                }

                if (nuevoVehiculo.getEstadoDisponibilidad() != EstadoDisponibilidad.DISPONIBLE) {
                    throw new WebApplicationException(
                            "El vehículo con placa " + nuevoVehiculo.getPlaca() + " no está disponible", 400);
                }

                reservaExistente.getVehiculo().setEstadoDisponibilidad(EstadoDisponibilidad.DISPONIBLE);

                nuevoVehiculo.setEstadoDisponibilidad(EstadoDisponibilidad.RESERVADO);
                reservaExistente.setVehiculo(nuevoVehiculo);
            }
        }

        if (reserva.getFechaInicio() != null) {
            reservaExistente.setFechaInicio(reserva.getFechaInicio());
        }
        if (reserva.getFechaFin() != null) {
            reservaExistente.setFechaFin(reserva.getFechaFin());
        }
        if (reserva.getTotal() != null) {
            reservaExistente.setTotal(reserva.getTotal());
        }
    }

    public ReservaVehiculo buscarReservaVehiculoId(Integer id) {
        ReservaVehiculo reserva = this.rri.findById(id);
        if (reserva == null)
            throw new WebApplicationException(
                    Response.status(Response.Status.NOT_FOUND)
                            .entity("Reserva no encontrada")
                            .type(MediaType.TEXT_PLAIN)
                            .build());
        return reserva;
    }

    public void eliminarReservaVehiculoId(Integer id) {
        ReservaVehiculo base = this.buscarReservaVehiculoId(id);
        if (base.getVehiculo() != null)
            base.getVehiculo().setEstadoDisponibilidad(EstadoDisponibilidad.DISPONIBLE);
        this.rri.deleteById(id);
    }

    public ReservaVehiculo buscarPorPlaca(String placa) {
        if (placa == null || placa.trim().isEmpty()) {
            throw new WebApplicationException("La placa para la búsqueda no puede estar vacía", 400);
        }

        ReservaVehiculo reserva = this.rri.find("vehiculo.placa", placa.trim()).firstResult();
        if (reserva == null) {
            throw new WebApplicationException("No existen reservas registradas con la placa: " + placa, 404);
        }
        return reserva;
    }


    public void cancelarReserva(Integer idReserva) {
        ReservaVehiculo reserva = this.rri.findById(idReserva);
        if (reserva == null) {
            throw new WebApplicationException("Reserva no encontrada", 404);
        }

        reserva.setEstado(EstadoDisponibilidad.CANCELADA);

        Vehiculo vehiculo = reserva.getVehiculo();
        if (vehiculo != null) {
            vehiculo.setEstadoDisponibilidad(EstadoDisponibilidad.DISPONIBLE);
        }

    }

    public void finalizarReserva(Integer idReserva) {
        ReservaVehiculo reserva = this.rri.findById(idReserva);
        if (reserva == null) {
            throw new WebApplicationException("Reserva no encontrada", 404);
        }

        reserva.setEstado(EstadoDisponibilidad.FINALIZADA);

        Vehiculo vehiculo = reserva.getVehiculo();
        if (vehiculo != null) {
            vehiculo.setEstadoDisponibilidad(EstadoDisponibilidad.DISPONIBLE);
        }
    }

    public void confirmarReserva(Integer idReserva) {
        ReservaVehiculo reserva = this.rri.findById(idReserva);
        if (reserva == null) {
            throw new WebApplicationException("Reserva no encontrada", 404);
        }

        reserva.setEstado(EstadoDisponibilidad.CONFIRMADA);

        Vehiculo vehiculo = reserva.getVehiculo();
        if (vehiculo != null) {
            vehiculo.setEstadoDisponibilidad(EstadoDisponibilidad.CONFIRMADA);
        }
    }
}