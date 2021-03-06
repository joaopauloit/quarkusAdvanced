package com.cantarino.ifood.cadastro.resources;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.cantarino.ifood.cadastro.dtos.restaurante.AddRestaurante;
import com.cantarino.ifood.cadastro.dtos.restaurante.EditRestaurante;
import com.cantarino.ifood.cadastro.dtos.restaurante.RestauranteDTO;
import com.cantarino.ifood.cadastro.infra.base.violations.ConstraintViolationResponse;
import com.cantarino.ifood.cadastro.infra.mappers.RestauranteMapper;
import com.cantarino.ifood.cadastro.models.Restaurante;

import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.OAuthFlow;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.security.OAuthFlows;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.SimplyTimed;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Path("/api/restaurantes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes (MediaType.APPLICATION_JSON)
@Tag(name =  "restaurantes")
@RolesAllowed("proprietario")
@SecurityScheme(securitySchemeName = "ifood-oauth", type = SecuritySchemeType.OAUTH2,
                flows = @OAuthFlows(password = @OAuthFlow(tokenUrl = "http://localhost:8180/auth/realms/ifood/protocol/openid-connect/token")))
@SecurityRequirement(name = "ifood-oauth")
public class RestauranteResource {


    @Inject
    RestauranteMapper  _mapper;

    @Inject
    @Channel("restaurantes")
    Emitter<String> emitter;

    @GET
    @Counted(name = "qtidadeBuscaRestaurante")
    @SimplyTimed(name = "Tempo de busca")
    @Timed(
        name = "tempo completo de busca"

    )
    public List<RestauranteDTO> buscar() {

        Stream<Restaurante> restaurantes = Restaurante.streamAll();
        return restaurantes.map(x -> _mapper.toRestauranteDTO(x))
                           .collect(Collectors.toList());

    }


    @POST
    @Transactional
    @APIResponse(responseCode = "200", description = "Caso restaurante seja cadastrado com sucesso")
    @APIResponse(responseCode = "400", content = @Content(schema = @Schema(allOf = ConstraintViolationResponse.class)))
    public Response adicionar(@Valid AddRestaurante dto) {

        Restaurante restaurante = _mapper.toRestaurante(dto);
        restaurante.persist();

        emitter.send(createJson(restaurante));

        return Response.status(Status.OK).build();
    }



    @PUT
    @Path("{id}")
    @Transactional
    public Response update(@PathParam("id")Long id, EditRestaurante dto) {

        Optional<Restaurante> _restaurante = Restaurante.findByIdOptional(id);
        if(!_restaurante.isPresent())
           throw new NotFoundException();


           Restaurante restaurante = _restaurante.get();
           _mapper.toRestaurante(dto,restaurante);

           restaurante.persist();
           return Response.status(Status.OK).build();

    }


    @DELETE
    @Path("{id}")
    @Transactional
    public Response delete(@PathParam("id")Long id) {

        Optional<Restaurante> _restaurante = Restaurante.findByIdOptional(id);
        _restaurante.ifPresentOrElse(Restaurante::delete, () -> { throw new NotFoundException(); });

        return Response.status(Status.OK).build();

    }



    private String createJson(Restaurante restaurante) {
        Jsonb create = JsonbBuilder.create();
        return  create.toJson(restaurante);
    }

}

