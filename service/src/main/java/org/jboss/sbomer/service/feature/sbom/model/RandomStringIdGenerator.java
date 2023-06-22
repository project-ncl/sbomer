package org.jboss.sbomer.service.feature.sbom.model;

import java.io.Serializable;
import java.util.UUID;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

/**
 * 
 */
public class RandomStringIdGenerator implements IdentifierGenerator {

	@Override
	public Serializable generate(SharedSessionContractImplementor session, Object object) throws HibernateException {
		return RandomStringIdGenerator.generate();
	}

	public static String generate() {
		return UUID.randomUUID().toString().replaceAll("_", "").replaceAll("-", "").substring(0, 15).toUpperCase();
	}

}
