package fi.ylihallila.server.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fi.ylihallila.server.commons.Roles;
import fi.ylihallila.server.hibernate.EnumSetType;
import fi.ylihallila.server.util.Constants;
import fi.ylihallila.server.util.Database;
import fi.ylihallila.server.util.PasswordHelper;
import fi.ylihallila.server.util.Util;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.InternalServerErrorResponse;
import org.apache.commons.validator.routines.EmailValidator;
import org.hibernate.Session;
import org.hibernate.annotations.*;
import org.hibernate.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Entity;
import javax.persistence.*;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Entity
@TypeDef(name = "enum-set", defaultForType = EnumSet.class, typeClass = EnumSetType.class)
@DiscriminatorValue("User")
public class User extends Owner {

    private transient final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Email for this user. Also used to identify user.
     */
    private String email;

    /**
     * Human readable presentation of @var organizationId;
     */
    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Organization organization;

    /**
     * If true, this user authenticates via OAuth and has no password.
     */
    @Column(nullable = true)
    private boolean oauth;

    /**
     * Password, encrypted with PBKDF2WithHmacSHA1.
     * @see fi.ylihallila.server.util.PasswordHelper
     */
    @JsonIgnore
    private String password;

    /**
     * Roles for this user.
     */
    @Column(length = 20)
    @Type(type = "enum-set", parameters = {
        @Parameter(
            name = "enumClass",
            value = "fi.ylihallila.server.commons.Roles"
        )
    })
    private EnumSet<Roles> roles;

    public User() {}

    public User(String id, String name, String email, EnumSet<Roles> roles, Organization organization) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.roles = roles;
        this.organization = organization;
    }

    public void setId(UUID id) {
        setId(id.toString());
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        if (name.length() >= 3) {
            this.name = name;
        } else {
            throw new BadRequestResponse("Username too short.");
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    /**
     * Sets the users email. This method validates that the email is valid.
     * @param email email to set
     * @throws BadRequestResponse if the email is already in use or is invalid
     */
    public void setEmail(String email) {
        if (Objects.equals(email, this.email)) {
            return;
        }

        EmailValidator validator = EmailValidator.getInstance(false, true);

        if (validator.isValid(email)) {
            Session session = Database.openSession();
            session.getTransaction().begin();

            Query query = session.createQuery("from User where email = :email", User.class);
            query.setParameter("email", email);

            int count = query.getResultList().size();

            session.getTransaction().commit();
            session.close();

            if (count == 0) {
                this.email = email;
            } else {
                throw new BadRequestResponse("Email already in use.");
            }
        } else {
            throw new BadRequestResponse("Invalid email");
        }
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(String id) {
        setOrganization(Util.getOrganization(id));
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public boolean usesOAuth() {
        return oauth;
    }

    public void setOAuth(boolean oauth) {
        this.oauth = oauth;
    }

    public String getPassword() {
        return password;
    }

    private void setPassword(String password) {
        this.password = password;
    }

    /**
     * Hashes the given passwords. Password required to be at least 5 characters long.
     * @param password password to hash
     * @throws BadRequestResponse when too password is too short
     * @throws InternalServerErrorResponse if password hashing fails
     */
    public void hashPassword(String password) {
        if (password.length() < 5) {
            throw new BadRequestResponse("Too short password. Password must be at least 5 characters long.");
        }

        try {
            this.password = PasswordHelper.generateStrongPassword(password).toString();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            logger.error("Error when hashing password", e);
            throw new InternalServerErrorResponse("Error when hashing password.");
        }
    }

    public EnumSet<Roles> getRoles() {
        return roles;
    }

    public void setRoles(EnumSet<Roles> roles) {
        this.roles = roles;
    }

    /**
     * Checks if the user has the specified role. If the user has the admin role, this will always return true.
     * @param role Role to check
     * @return true if the user has the specified role or admin role
     */
    public boolean hasRole(Roles role) {
        return getRoles().contains(role) || getRoles().contains(Roles.ADMIN);
    }

    /**
     * Each user has <b>one</b> personal workspace, where they can create their own subjects and projects.
     * This method creates the workspace if it does not exist.
     */
    @Transient
    @JsonIgnore
    public Workspace getPersonalWorkspace() {
        Session session = Database.openSession();
        session.getTransaction().begin();

        try {
            return session.createQuery("from Workspace where owner.id = :id", Workspace.class)
                    .setParameter("id", getId())
                    .getSingleResult();
        } catch (NoResultException e) {
            Workspace workspace = new Workspace(getName() + "'s Personal Workspace", this);
            session.save(workspace);

            return workspace;
        } catch (Exception e) {
            throw new InternalServerErrorResponse(e.getMessage());
        } finally {
            session.getTransaction().commit();
            session.close();
        }
    }

    /**
     * All copied projects are saved into <i>Personal Projects</i> subject. There should only be one of these.
     * This method creates the subject if it does note exist.
     */
    @Transient
    @JsonIgnore
    public Subject getCopiedProjectsSubject() {
        Workspace personalWorkspace = getPersonalWorkspace();

        Session session = Database.openSession();
        session.getTransaction().begin();

        // TODO: What happens when the user renames a new subject as `Personal Projects`
        Optional<Subject> possibleSubject = personalWorkspace.findSubject(Constants.COPIED_PROJECTS_NAME);

        if (possibleSubject.isPresent()) {
            return possibleSubject.get();
        }

        Subject subject = new Subject(Constants.COPIED_PROJECTS_NAME, personalWorkspace);
        session.save(subject);

        session.getTransaction().commit();
        session.close();

        return subject;
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", organization='" + organization + '\'' +
                ", roles=" + roles +
                '}';
    }
}


