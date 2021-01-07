package fi.ylihallila.server.util;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.type.UUIDCharType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class Database {

	private static final Logger logger = LoggerFactory.getLogger(Database.class);
	private static SessionFactory sessionFactory = null;

	static {
		StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
			.configure()
			.build();

		try {
			MetadataSources sources = new MetadataSources(registry);
			Metadata metadata = sources.getMetadataBuilder()
					.applyBasicType(UUIDCharType.INSTANCE, UUID.class.getName())
					.build();

			sessionFactory = metadata.buildSessionFactory();
		} catch (Exception e) {
			StandardServiceRegistryBuilder.destroy(registry);
			logger.error("Error while creating database", e);
			logger.error("Cannot continue without database, exiting ...");
			System.exit(0);
		}
	}

	/**
	 * Creates a new session.
	 * @return Session
	 */
	public static Session getSession() {
		return sessionFactory.openSession();
	}
}
