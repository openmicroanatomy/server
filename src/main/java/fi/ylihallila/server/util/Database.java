package fi.ylihallila.server.util;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Database {

	private static final Logger logger = LoggerFactory.getLogger(Database.class);
	private static SessionFactory sessionFactory = null;

	static {
		StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
			.configure()
			.build();

		try {
			sessionFactory = new MetadataSources(registry).buildMetadata().buildSessionFactory();
		} catch (Exception e) {
			logger.error("Error while creating database", e);
			StandardServiceRegistryBuilder.destroy(registry);
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
