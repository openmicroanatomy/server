package fi.ylihallila.server.models;

import javax.persistence.*;
import java.util.Objects;
import java.util.UUID;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class Owner {

	@Id
	@Column(name = "id", updatable = false, nullable = false)
	/* We cannot use @GeneratedValue here, because Hibernate would override our manually provided UUID's */
	public String id;

	public String name;

	public Owner() {}

	public Owner(String id, String name) {
		this.id = id;
		this.name = name;
	}

	public void setId(UUID id) {
		this.setId(id.toString());
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Owner owner = (Owner) o;
		return Objects.equals(id, owner.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public String toString() {
		return "Owner{" +
				"id='" + id + '\'' +
				", name='" + name + '\'' +
				'}';
	}
}
