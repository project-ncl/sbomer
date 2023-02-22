package org.redhat.sbomer.rest;

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

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.redhat.sbomer.model.SBOM;
import org.redhat.sbomer.service.SBOMService;
import org.redhat.sbomer.validation.ValidationError;

@Path("/sboms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "SBOMs", description = "Endpoints related to SBOM handling")
public class SBOMResource {

    @Inject
    SBOMService sbomService;

    @Inject
    Validator validator;

    /**
     * Make it possible to create a {@link SBOM} resource directly from the
     * endpoint.
     * 
     * TODO: We probably shouldn't be really exposing this endpoint, but it's
     * convenient at development.
     * 
     * @param sbom
     * @return
     */
    @POST
    @Operation(summary = "Create SBOM", description = "Save submitted SBOM. This endpoint expects an SBOM in the CycloneDX format serialized to JSON.")
    public Response take(final SBOM sbom) {
        Set<ConstraintViolation<SBOM>> violations = validator.validate(sbom);

        if (!violations.isEmpty()) {
            return Response.status(Status.BAD_REQUEST).entity(new ValidationError(violations)).build();
        }

        sbomService.saveBom(sbom);

        return Response.status(Status.CREATED).entity(sbom).build();
    }

    @POST
    @Operation(summary = "Create SBOM based on the PNC build", description = "SBOM generation for a particular PNC build Id offloaded to the service")
    @Path("{id}")
    public Response fromBuild(@PathParam("id") String id) throws Exception {

        sbomService.createBomFromPncBuild(id);

        // Nothing is happening, yet!
        return Response.status(Status.ACCEPTED).build();
    }

    @GET
    @Operation(summary = "List of all SBOMs", description = "List all SBOMs available in the system")
    public List<SBOM> list() {
        return sbomService.listBoms();
    }

    @GET
    @Path("{id}")
    @Operation(summary = "Get specific SBOM", description = "List all SBOMs available in the system")
    public SBOM get(@PathParam("id") String id) {
        return sbomService.getBom(id);
    }
}