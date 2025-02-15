package fi.ylihallila.server.models;

import com.fasterxml.jackson.annotation.*;
import fi.ylihallila.server.commons.Roles;
import fi.ylihallila.server.jackson.Filters;
import fi.ylihallila.server.util.Util;
import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.*;

@Entity
@Table( name = "workspaces" )
@JsonIdentityInfo(scope = Workspace.class, generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@JsonFilter("ReadWriteFilter")
public class Workspace {

	/**
	 * UUID representing this workspace.
	 */
	@Id
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(
		name = "UUID",
		strategy = "org.hibernate.id.UUIDGenerator"
	)
	@Column(name = "id", updatable = false, nullable = false)
	private String id;

	/**
	 * Workspace name. Visible in QuPath.
	 */
	private String name;

	/**
	 * UUID of the workspace owner, either an organization or a user.
	 */
	@ManyToOne
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Owner owner;

	/**
	 * Organizations / users who are allowed to read this workspace. If null, the workspace is public and available
	 * to everyone. If an organization, only authenticated users with that organization can view this workspace.
	 */
	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "read", joinColumns = @JoinColumn(name = "workspace_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
	@Filters.VisibleToWriteOnly
	private final Set<Owner> read = new HashSet<>();

	/**
	 * Organizations / users who are allowed to write to this workspace. Users with write permission are also allowed to read.
	 * By default, this is set to the user who created this workspace.
	 */
	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "write", joinColumns = @JoinColumn(name = "workspace_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
	@Filters.VisibleToWriteOnly
	private final Set<Owner> write = new HashSet<>();

	@OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private List<Subject> subjects = new ArrayList<>();

	public Workspace() {}

	public Workspace(String name, Owner owner) {
		this(UUID.randomUUID().toString(), name, owner, List.of());
	}

	public Workspace(String id, String name, Owner owner, List<Subject> subjects) {
		this.id = id;
		this.name = name;
		this.owner = owner;
		this.subjects = subjects;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(UUID uuid) {
		setId(uuid.toString());
	}

	public void setId(String id) {
		this.id = id;
	}

	public Owner getOwner() {
		return owner;
	}

	public void setOwner(String id) {
		setOwner(Util.getOrganization(id));
	}

	public void setOwner(Owner owner) {
		this.owner = owner;
	}

	public List<Subject> getSubjects() {
		return subjects;
	}

	public void setSubjects(List<Subject> projects) {
		this.subjects = projects;
	}

	public void addSubject(Subject subject) {
		subject.setWorkspace(this);
		this.subjects.add(subject);
	}

	public void removeSubject(Subject subject) {
		subjects.remove(subject);
		subject.setWorkspace(null);
		// TODO: Delete subject?
	}

	public Optional<Subject> findSubject(String name) {
		return subjects.stream()
				.filter(subject -> subject.getName().equalsIgnoreCase(name))
				.findFirst();
	}

	public void setWritePermissions(Collection<Owner> owners) {
		this.write.clear();
		this.write.addAll(owners);
	}

	public boolean hasWritePermission(Owner owner) {
		if (owner instanceof User user) {
            if (user.hasRole(Roles.ADMIN) || this.write.contains(user.getOrganization())) {
				return true;
			}
		}

		return this.write.contains(owner);
	}

	@JsonProperty("write")
	public Set<Owner> getUsersWithWritePermission() {
		return Set.copyOf(write);
	}

	public void addReadPermission(Owner... owners) {
		this.read.addAll(Arrays.asList(owners));
	}

	public void addReadPermission(Collection<Owner> owners) {
		this.read.addAll(owners);
	}

	public void addReadPermission(Owner owner) {
		this.read.add(owner);
	}

	public void setReadPermissions(Collection<Owner> owners) {
		this.read.clear();
		this.read.addAll(owners);
	}

	public void removeReadPermission(Owner owner) {
		this.read.remove(owner);
	}

	public boolean hasReadPermission(Owner owner) {
		if (this.read.isEmpty()) {
			return true;
		}

		if (hasWritePermission(owner)) {
			return true;
		}

		if (owner instanceof User) {
			User user = (User) owner;

			if (user.hasRole(Roles.ADMIN) || this.read.contains(user.getOrganization())) {
				return true;
			}
		}

		return this.read.contains(owner);
	}

	@JsonProperty("read")
	public Set<Owner> getUsersWithReadPermission() {
		return Set.copyOf(read);
	}

	@Override
	public String toString() {
		return "Workspace{" +
				"id='" + id + '\'' +
				", name='" + name + '\'' +
				", owner=" + owner +
				", read=" + read +
				", write=" + write +
				", subjects=" + subjects +
				'}';
	}
}
