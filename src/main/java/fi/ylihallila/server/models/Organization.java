package fi.ylihallila.server.models;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

//@Embeddable
@Entity
@Table( name = "organizations" )
public class Organization extends Owner {

	@Id
	@Column(name = "id", updatable = false, nullable = false)
	private String id;

	private String name;

	public Organization() {}

	public Organization(String id, String name) {
		this.id = id;
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "Organization{" +
				"id='" + id + '\'' +
				", name='" + name + '\'' +
				'}';
	}
}

