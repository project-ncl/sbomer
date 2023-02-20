package org.redhat.sbomer;

import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.cyclonedx.model.Bom;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.redhat.sbomer.model.SBOM;
import org.redhat.sbomer.model.ValidationError;
import org.redhat.sbomer.service.SBOMService;

@Path("/sboms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "SBOMs", description = "Endpoints related to SBOM handling")
public class SBOMResource {

    @Inject
    SBOMService service;

    @Inject
    Validator validator;

    @POST
    @Operation(summary = "SBOM creation entrypoint", description = "Save submitted SBOM. This endpoint expects an SBOM in the CycloneDX format serialized to JSON.")
    public Response take(final SBOM sbom) {
        Set<ConstraintViolation<SBOM>> violations = validator.validate(sbom);

        System.out.println(violations);

        if (!violations.isEmpty()) {
            return Response.status(Status.BAD_REQUEST).entity(new ValidationError(violations)).build();
        }

        service.create(sbom);

        return Response.status(Status.CREATED).entity(sbom).build();

    }

    @GET
    @Operation(summary = "List of all SBOMs", description = "List all SBOMs available in the system")
    public List<SBOM> list() {
        return service.list();
    }

    @GET
    @Path("{id}")
    @Operation(summary = "Get specific SBOM", description = "List all SBOMs available in the system")
    public Bom get(@PathParam("id") String id) {
        return null;
    }
}