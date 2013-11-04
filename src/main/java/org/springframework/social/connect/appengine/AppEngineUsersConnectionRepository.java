/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.social.connect.appengine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.appengine.api.datastore.Key;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.ConnectionKey;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.ConnectionSignUp;
import org.springframework.social.connect.UsersConnectionRepository;
import org.springframework.social.connect.appengine.DatastoreUtils.EntityMapper;
import org.springframework.social.connect.intercept.ConnectionInterceptor;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.CompositeFilter;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.FilterOperator;

/**
 * {@link UsersConnectionRepository} that uses the AppEngine Datastore API to persist connection data.
 * @author Vladislav Tserman
 */
public class AppEngineUsersConnectionRepository implements UsersConnectionRepository {

	private transient final DatastoreService datastore;
	
	private transient final ConnectionFactoryLocator connectionFactoryLocator;
	private transient final TextEncryptor textEncryptor;
	private transient ConnectionSignUp connectionSignUp;
	private transient final List<ConnectionInterceptor<?>> interceptors = new ArrayList<ConnectionInterceptor<?>>();

    private transient String kindPrefix = "";

	public AppEngineUsersConnectionRepository(ConnectionFactoryLocator connectionFactoryLocator, TextEncryptor textEncryptor, DatastoreService datastore) {
		this.connectionFactoryLocator = connectionFactoryLocator;
		this.textEncryptor = textEncryptor;
		this.datastore = datastore;
	}
	
	/**
	 * The command to execute to create a new local user profile in the event no user id could be mapped to a connection.
	 * Allows for implicitly creating a user profile from connection data during a provider sign-in attempt.
	 * Defaults to null, indicating explicit sign-up will be required to complete the provider sign-in attempt.
	 * @see #findUserIdsWithConnection(Connection)
	 */
	public void setConnectionSignUp(ConnectionSignUp connectionSignUp) {
		this.connectionSignUp = connectionSignUp;
	}
	
	/**
	 * Sets a datastore entity kind prefix. This will be prefixed to all the kind names before queries are executed. Defaults to "".
	 * This is can be used to distinguish Spring Social entities from other application tables. 
	 * @param kindPrefix the kindPrefix to set
	 */
	public void setKindPrefix(String kindPrefix) {
		this.kindPrefix = kindPrefix;
	}
	
	/** Returns kind of the entity */
	private String getKind() {
		return (kindPrefix != null ? kindPrefix : "") + "UserConnection";
	}
	
	
	@Override
	public List<String> findUserIdsWithConnection(Connection<?> connection) {
		ConnectionKey key = connection.getKey();
		final CompositeFilter filter = CompositeFilterOperator.and(
			FilterOperator.EQUAL.of("providerId", key.getProviderId()),
			FilterOperator.EQUAL.of("providerUserId", key.getProviderUserId())
		);
        // fetch and return only keys, not full entities.
		Query query = new Query(getKind()).setFilter(filter).setKeysOnly();
		List<String> localUserIds = DatastoreUtils.queryForList(datastore.prepare(query), userIdMapper);
		if (localUserIds.size() == 0 && connectionSignUp != null) {			
				String newUserId = connectionSignUp.execute(connection);
				if (newUserId != null) {
					createConnectionRepository(newUserId).addConnection(connection);
					return Arrays.asList(newUserId);
				}
		}
		return localUserIds;
	}

	@Override
	public Set<String> findUserIdsConnectedTo(String providerId, Set<String> providerUserIds) {
		final CompositeFilter filter = CompositeFilterOperator.and(
			FilterOperator.EQUAL.of("providerId", providerId),
			FilterOperator.IN.of("providerUserId", providerUserIds)
		);
        // fetch and return only keys, not full entities.
		Query query = new Query(getKind()).setFilter(filter).setKeysOnly();
		List<String> resultList = DatastoreUtils.queryForList(datastore.prepare(query), userIdMapper);
		return new HashSet<String>(resultList);
	}
	
	@Override
	public ConnectionRepository createConnectionRepository(String userId) {
		if (userId == null) throw new IllegalArgumentException("userId cannot be null");
		AppEngineConnectionRepository repo = new AppEngineConnectionRepository(userId, connectionFactoryLocator, textEncryptor, datastore, kindPrefix);
		repo.setInterceptors(interceptors);
		return repo;
	}

	private final EntityMapper<String> userIdMapper = new EntityMapper<String>() {
		public String mapEntity(Entity entity) {
            Key key = entity.getKey().getParent();
            return key.getName();
		}
	};

	/**
	 * Configure the list of interceptors that should receive callbacks during the connection CRUD operations.
	 * @param interceptors the connect interceptors to add
	 */
	public void setInterceptors(List<ConnectionInterceptor<?>> interceptors) {
		this.interceptors.addAll(interceptors);
	}
	
	/**
	 * Adds a ConnectionInterceptor to receive callbacks during the connection CRUD operations.
	 * @param interceptor the connection interceptor to add
	 */
	public void addInterceptor(ConnectionInterceptor<?> interceptor) {
		interceptors.add(interceptor);
	}	
}
