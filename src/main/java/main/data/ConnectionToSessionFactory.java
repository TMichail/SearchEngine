package main.data;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

public class ConnectionToSessionFactory {
    private static volatile SessionFactory sessionFactory;

    private ConnectionToSessionFactory() {
    }

    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            synchronized (ConnectionToSessionFactory.class) {
                if (sessionFactory == null) {
                    StandardServiceRegistry registry = new StandardServiceRegistryBuilder().
                            configure("hibernate.cfg.xml").build();
                    Metadata metadata = new MetadataSources(registry).getMetadataBuilder().build();
                    sessionFactory = metadata.getSessionFactoryBuilder().build();
                }
            }
        }
        return sessionFactory;
    }
}
