package fi.ylihallila.server.controllers;

import fi.ylihallila.server.authentication.Authenticator;
import fi.ylihallila.server.commons.Roles;
import fi.ylihallila.server.exceptions.UnprocessableEntityResponse;
import fi.ylihallila.server.models.Organization;
import fi.ylihallila.server.models.User;
import fi.ylihallila.server.util.Constants;
import io.javalin.apibuilder.CrudHandler;
import io.javalin.core.util.FileUtil;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import io.javalin.http.UploadedFile;
import io.javalin.plugin.openapi.annotations.*;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class OrganizationController extends Controller implements CrudHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @OpenApi(
        summary = "Create a new organization",
        tags = { "organization" },
        formParams = {
            @OpenApiFormParam(name = "name", required = true)
        },
        responses = {
            @OpenApiResponse(status = "201", content = @OpenApiContent(from = Organization.class)),
            @OpenApiResponse(status = "403"),
        }
    )
    @Override public void create(@NotNull Context ctx) {
        Allow(ctx, Roles.ADMIN);

        String name = ctx.formParam("name", String.class).get();
        String id   = UUID.randomUUID().toString();

        Session session = ctx.use(Session.class);
        User user = Authenticator.getUser(ctx);

        Organization organization = new Organization();
        organization.setId(id);
        organization.setName(name);
        session.save(organization);

        ctx.status(201).json(organization);

        logger.info("Organization {} ({}) created by {}", name, id, user.getName());
    }

    @OpenApi(
        summary = "Delete given organization",
        tags = { "organization" },
        pathParams = @OpenApiParam(
            name = "id",
            description = "UUID of organization to be deleted",
            required = true
        ),
        responses = {
            @OpenApiResponse(status = "200"),
            @OpenApiResponse(status = "403"),
            @OpenApiResponse(status = "404")
        }
    )
    @Override public void delete(@NotNull Context ctx, @NotNull String id) {
        Allow(ctx, Roles.ADMIN);

        Session session = ctx.use(Session.class);
        User user = Authenticator.getUser(ctx);

        Organization organization = getOrganization(ctx, id);
        session.delete(organization);

        logger.info("Organization {} ({}) deleted by {}", organization.getName(), organization.getId(), user.getName());
    }

    @OpenApi(
        summary = "Get all organizations",
        tags = { "organization" },
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = Organization.class, isArray = true))
        }
    )
    @Override public void getAll(@NotNull Context ctx) {
        Session session = ctx.use(Session.class);
        List<Organization> organizations = session.createQuery("from Organization ", Organization.class).list();
        organizations.sort(Comparator.comparing(Organization::getName));

        ctx.status(200).json(organizations);
    }

    @OpenApi(
        summary = "Get given organization",
        tags = { "organization" },
        pathParams = @OpenApiParam(
            name = "id",
            description = "UUID of organization to be fetched",
            required = true
        ),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = Organization.class)),
            @OpenApiResponse(status = "403"),
            @OpenApiResponse(status = "404")
        }
    )
    @Override public void getOne(@NotNull Context ctx, @NotNull String id) {
        Organization organization = getOrganization(ctx, id);

        ctx.status(200).json(organization);
    }

    @OpenApi(
        summary = "Update given organization",
        tags = { "organization" },
        pathParams = @OpenApiParam(
            name = "id",
            description = "UUID of organization to be updated",
            required = true
        ),
        formParams = {
            @OpenApiFormParam(name = "name"),
            @OpenApiFormParam(name = "logo", type = File.class)
        }
    )
    @Override public void update(@NotNull Context ctx, @NotNull String id) {
        Allow(ctx, Roles.ADMIN);
        User user = Authenticator.getUser(ctx);

        Organization organization = getOrganization(ctx, id);
        organization.setName(ctx.formParam("name", organization.getName()));

        UploadedFile file = ctx.uploadedFile("logo");

        if (file != null) {
            if (!isValidOrganizationLogo(file.getContent())) {
                throw new UnprocessableEntityResponse("Provided file was not an PNG file or is too small.");
            }

            FileUtil.streamToFile(file.getContent(), String.format(Constants.ORGANIZATION_LOGOS, id));
        }

        logger.info("Organization {} ({}) edited by {}", organization.getName(), id, user.getName());
    }

    /*  PRIVATE API   */

    private Organization getOrganization(Context ctx, String id) {
        Session session = ctx.use(Session.class);
        Organization organization = session.find(Organization.class, id);

        if (organization == null) {
            throw new NotFoundResponse();
        }

        return organization;
    }
}
