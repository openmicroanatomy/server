package fi.ylihallila.server.tests;

import fi.ylihallila.server.commons.Roles;
import fi.ylihallila.server.util.Database;
import fi.ylihallila.server.models.*;
import org.hibernate.Session;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Dummy Database consists of two projects (Project A and Project B), two organizations (A and B)
 * and two workspaces (A and B).
 *
 * Project A is in Workspace A, belonging to Organization A. Project B respectively in B, belonging to B.
 *
 * Each workspace owns one slide.
 *
 * Two users (Admin and Teacher) exist; both in Organization A.
 */
public class DummyDb {

    public static void create() {
        Session session = Database.getSession();
        session.beginTransaction();

        session.save(ORGANIZATION_A);
        session.save(ORGANIZATION_B);

        session.save(WORKSPACE_A);
        session.save(WORKSPACE_B);

        session.save(SUBJECT_A);
        session.save(SUBJECT_B);

        WORKSPACE_A.setSubjects(List.of(SUBJECT_A));
        WORKSPACE_B.setSubjects(List.of(SUBJECT_B));

        session.save(SLIDE_A);
        session.save(SLIDE_B);

        session.save(PROJECT_A);
        session.save(PROJECT_B);

        session.getTransaction().commit();
        session.close();
    }

    public static Organization ORGANIZATION_A = new Organization(
        "0d9e61c5-3642-4f3a-bf26-58ce7852ae33",
        "Organization A"
    );

    public static Organization ORGANIZATION_B = new Organization(
        "962c10fa-caab-4c09-908a-1a67152a3e15",
        "Organization B"
    );

    // THESE USERS ARE DEFINED IN USERS.JSON AND ARE AUTOMATICALLY INSERTED TO THE DATABASE!
    // These objects are here only to make unit testing easier

    public static User TEACHER = new User(
        "70e99eac-b439-4a73-967e-2d83870b8326",
        "Teacher",
        "teacher@example.com",
        EnumSet.of(Roles.MANAGE_USERS, Roles.MANAGE_SLIDES, Roles.MANAGE_PROJECTS, Roles.MANAGE_PERSONAL_PROJECTS),
        ORGANIZATION_A
    );

    public static User ADMIN = new User(
        "aeae48ac-961a-425e-a715-c01205d2e83d",
        "Admin",
        "admin@example.com",
        EnumSet.of(Roles.MANAGE_USERS, Roles.MANAGE_SLIDES, Roles.MANAGE_PROJECTS, Roles.ADMIN, Roles.MANAGE_PERSONAL_PROJECTS),
        ORGANIZATION_A
    );

    public static Workspace WORKSPACE_A = new Workspace(
        "cf869d47-686e-4e69-bc20-5a233feb2a54",
        "Workspace A",
        ORGANIZATION_A,
        null
    );

    public static Workspace WORKSPACE_B = new Workspace(
        "958af329-05cf-4a2a-9321-9a603d0ac6d7",
        "Workspace B",
        ORGANIZATION_B,
        null
    );

    public static Subject SUBJECT_A = new Subject(
        "Subject A in workspace A",
        WORKSPACE_A
    );

    public static Subject SUBJECT_B = new Subject(
        "Subject A in workspace B",
        WORKSPACE_B
    );

    public static Project PROJECT_A = new Project(
        "bd818664-6896-4632-933d-af3e59c43c36",
        "Project A",
        "Project A description",
        SUBJECT_A
    );

    public static Project PROJECT_B = new Project(
        "87babe71-3f7a-48d7-9c6e-28c617b8d0ee",
        "Project B",
        "Project B description",
        SUBJECT_B
    );

    public static Slide SLIDE_A = new Slide(
        "09b6d682-8964-427f-af06-956d7fe69622",
        "Slide A",
        ORGANIZATION_A
    );

    public static Slide SLIDE_B = new Slide(
        "802d68e4-8afc-4cbf-99d8-c8ed129a8d20",
        "Slide B",
        ORGANIZATION_B
    );

    public static Slide SLIDE_DELETE = new Slide(
        "3d9a3fd2-4dec-4745-bb3c-7662ff6d9c94",
        "Slide B",
        ORGANIZATION_A
    );
}
