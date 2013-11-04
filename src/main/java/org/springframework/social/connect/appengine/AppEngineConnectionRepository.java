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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.springframework.core.GenericTypeResolver;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionData;
import org.springframework.social.connect.ConnectionFactory;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.ConnectionKey;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.DuplicateConnectionException;
import org.springframework.social.connect.NoSuchConnectionException;
import org.springframework.social.connect.NotConnectedException;
import org.springframework.social.connect.appengine.DatastoreUtils.EntityMapper;
import org.springframework.social.connect.intercept.ConnectionInterceptor;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery.TooManyResultsException;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.CompositeFilter;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.Transaction;
import static com.google.appengine.api.datastore.TransactionOptions.Builder.*;


/** @author Vladislav Tserman */
class AppEngineConnectionRepository implements ConnectionRepository {
	private static final Logger log = Logger.getLogger(AppEngineConnectionRepository.class.getName());
	
	private final DatastoreService datastore;	
	private final String userId;
	private final ConnectionFactoryLocator connectionFactoryLocator;
	private final TextEncryptor textEncryptor;		
	private final String kindPrefix;
	private final MultiValueMap<Class<?>, ConnectionInterceptor<?>> interceptors = new LinkedMultiValueMap<Class<?>, ConnectionInterceptor<?>>();
	private final Key userKey;
		
	public AppEngineConnectionRepository(String userId, ConnectionFactoryLocator connectionFactoryLocator, 
			TextEncryptor textEncryptor, DatastoreService datastore, String kindPrefix)
	{
		this.userId = userId;
		this.connectionFactoryLocator = connectionFactoryLocator;
		this.textEncryptor = textEncryptor;
		this.datastore = datastore;
		this.kindPrefix = kindPrefix;
        this.userKey = KeyFactory.createKey(getParentKind(), userId);
	}
	
	private String getParentKind() {
		return (kindPrefix != null ? kindPrefix : "") + "User"; 
	}
	
	/** Returns kind of the entity */
	private String getKind() {		
		return (kindPrefix != null ? kindPrefix : "") + "UserConnection";
	}
	
	@Override
	public MultiValueMap<String, Connection<?>> findAllConnections() {
		MultiValueMap<String, Connection<?>> connections = new LinkedMultiValueMap<String, Connection<?>>();
		Set<String> registeredProviderIds = connectionFactoryLocator.registeredProviderIds();
		for (String registeredProviderId : registeredProviderIds) {
			connections.put(registeredProviderId, Collections.<Connection<?>>emptyList());
		}		
		Query query = new Query(getKind())
			.setAncestor(userKey)
			.addSort("providerId")
			.addSort("rank");		
		List<Connection<?>> resultList = DatastoreUtils.queryForList(datastore.prepare(query), connectionMapper);
		for (Connection<?> connection : resultList) {
			String providerId = connection.getKey().getProviderId();
			if (connections.get(providerId).size() == 0) {
				connections.put(providerId, new LinkedList<Connection<?>>());
			}
			connections.add(providerId, connection);
		}
		return connections;
	}

	
	@Override
	public List<Connection<?>> findConnections(String providerId) {
		Query query = new Query(getKind())
			.setAncestor(userKey)
			.setFilter(FilterOperator.EQUAL.of("providerId", providerId))
			.addSort("rank");
		return DatastoreUtils.queryForList(datastore.prepare(query), connectionMapper);
	}

	
	@Override
	@SuppressWarnings("unchecked")
	public <A> List<Connection<A>> findConnections(Class<A> apiType) {
		List<?> connections = findConnections(getProviderId(apiType));
		return (List<Connection<A>>) connections;
	}

	
	@Override
	public MultiValueMap<String, Connection<?>> findConnectionsToUsers(MultiValueMap<String, String> providerUserIds) {
		if (providerUserIds.isEmpty()) {
			throw new IllegalArgumentException("Unable to execute find: no providerUsers provided");
		}
		
		final Set<Entity> queryResultSet = new TreeSet<Entity>(new Comparator<Entity>() {
			@Override
			public int compare(Entity e1, Entity e2) {
				String providerId1 = (String) e1.getProperty("providerId");
				String providerId2 = (String) e2.getProperty("providerId");
				int compareToResult = providerId1.compareTo(providerId2);
				if (compareToResult != 0) return compareToResult;
				Long rank1 = (Long) e1.getProperty("rank");
				Long rank2 = (Long) e2.getProperty("rank");
				return rank1.compareTo(rank2);
			}
		});
		
		//FIXME find more optimal way to query datastore
		for (Entry<String, List<String>> entry : providerUserIds.entrySet()) {
			String providerId = entry.getKey();
			final CompositeFilter filter = CompositeFilterOperator.and(
				FilterOperator.EQUAL.of("providerId", providerId),
				FilterOperator.IN.of("providerUserId", entry.getValue())
			);
			final Query query = new Query(getKind())
				.setAncestor(userKey)
				.setFilter(filter)
				.addSort("providerId")
				.addSort("rank");
			queryResultSet.addAll(datastore.prepare(query).asList(
					FetchOptions.Builder.withDefaults()));
		}
		
		List<Connection<?>> resultList = new ArrayList<Connection<?>>(queryResultSet.size());
		for (Entity entity : queryResultSet) {
			resultList.add(connectionMapper.mapEntity(entity));
		}
		
		MultiValueMap<String, Connection<?>> connectionsForUsers = new LinkedMultiValueMap<String, Connection<?>>();
		for (Connection<?> connection : resultList) {
			String providerId = connection.getKey().getProviderId();
			List<String> userIds = providerUserIds.get(providerId);
			List<Connection<?>> connections = connectionsForUsers.get(providerId);
			if (connections == null) {
				connections = new ArrayList<Connection<?>>(userIds.size());
                for (String userId : userIds) {
                    connections.add(null);
                }
				connectionsForUsers.put(providerId, connections);
			}
			String providerUserId = connection.getKey().getProviderUserId();
			int connectionIndex = userIds.indexOf(providerUserId);
			connections.set(connectionIndex, connection);
		}
		return connectionsForUsers;
	}

	
	@Override
	public Connection<?> getConnection(ConnectionKey connectionKey) {
		final CompositeFilter filter = CompositeFilterOperator.and(
			FilterOperator.EQUAL.of("providerId", connectionKey.getProviderId()),
			FilterOperator.EQUAL.of("providerUserId", connectionKey.getProviderUserId())
		);
		final Query query = new Query(getKind())
			.setAncestor(userKey)
			.setFilter(filter);
		Entity singleEntity;
		try {
			singleEntity = datastore.prepare(query).asSingleEntity();			
		} catch (TooManyResultsException ex) {
			log.warning("Too many results were found for query " + query.toString());
			throw new NoSuchConnectionException(connectionKey);
		}
		if (singleEntity == null) throw new NoSuchConnectionException(connectionKey);
		return connectionMapper.mapEntity(singleEntity);
	}

	
	@SuppressWarnings("unchecked")
	@Override
	public <A> Connection<A> getConnection(Class<A> apiType, String providerUserId) {
		String providerId = getProviderId(apiType);
		return (Connection<A>) getConnection(new ConnectionKey(providerId, providerUserId));
	}

	
	@SuppressWarnings("unchecked")
	@Override
	public <A> Connection<A> getPrimaryConnection(Class<A> apiType) {
		String providerId = getProviderId(apiType);
		Connection<A> connection = (Connection<A>) findPrimaryConnection(providerId);
		if (connection == null) {
			throw new NotConnectedException(providerId);
		}
		return connection;
	}
	
	
	private Connection<?> findPrimaryConnection(String providerId) {
		final CompositeFilter filter = CompositeFilterOperator.and(
			FilterOperator.EQUAL.of("providerId", providerId), 
			FilterOperator.EQUAL.of("rank", 1L)
		);
		final Query query = new Query(getKind())
			.setAncestor(userKey)
			.setFilter(filter);
		List<Connection<?>> resultList = DatastoreUtils.queryForList(
				datastore.prepare(query), connectionMapper);
		return resultList.size() > 0 ? resultList.get(0) : null;				
	}

	
	@SuppressWarnings("unchecked")
	@Override
	public <A> Connection<A> findPrimaryConnection(Class<A> apiType) {
		String providerId = getProviderId(apiType);
		return (Connection<A>) findPrimaryConnection(providerId);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void addConnection(Connection<?> connection) {
		ConnectionData data = connection.createData();				
		long rank = 1;
		// Find max rank -> Query sorting by rank in desc order and limiting resultset to single entity
		final Query query = new Query(getKind())
			.setAncestor(userKey)
			.setFilter(FilterOperator.EQUAL.of("providerId", data.getProviderId()))
			.addSort("rank", SortDirection.DESCENDING);
		List<Entity> resultList = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));
		Entity singleEntity = (resultList != null && resultList.size() > 0) ? resultList.get(0) : null;
		if (singleEntity != null) rank += (Long) singleEntity.getProperty("rank");
		
		String connectionKeyName = createConnectionKeyName(userId, connection.getKey());
		Entity userConnection = new Entity(getKind(), connectionKeyName, userKey);
		//userConnection.setProperty("userId", userId);
		userConnection.setProperty("providerId", data.getProviderId());
		userConnection.setProperty("providerUserId", data.getProviderUserId());
		userConnection.setProperty("rank", rank);
		userConnection.setProperty("displayName", data.getDisplayName());
		userConnection.setProperty("profileUrl", data.getProfileUrl());
		userConnection.setProperty("imageUrl", data.getImageUrl());
		userConnection.setProperty("accessToken", encrypt(data.getAccessToken()));
		userConnection.setProperty("secret", encrypt(data.getSecret()));
		userConnection.setProperty("refreshToken", encrypt(data.getRefreshToken()));
		userConnection.setProperty("expireTime", data.getExpireTime());		
				
		for (ConnectionInterceptor interceptor : interceptingConnectionsTo(connection)) {
			interceptor.beforeCreate(userId, connection);
		}
		
		final Key key = new KeyFactory.Builder(userKey).addChild(getKind(), connectionKeyName).getKey();
		Transaction txn = datastore.beginTransaction();
		try {			
			try {
				datastore.get(txn, key);
				throw new DuplicateConnectionException(connection.getKey());
			} catch (EntityNotFoundException e) {
				datastore.put(txn, userConnection);
				txn.commit();
			}
		} finally {
			if (txn.isActive()) txn.rollback();
		}
				
		for (ConnectionInterceptor interceptor : interceptingConnectionsTo(connection)) {
			interceptor.afterCreate(userId, connection);
		}
	}

	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void updateConnection(Connection<?> connection) {
		ConnectionData data = connection.createData();
		
		for (ConnectionInterceptor interceptor : interceptingConnectionsTo(connection)) {
			interceptor.beforeUpdate(userId, connection);
		}
		String connectionKeyName = createConnectionKeyName(userId, connection.getKey());
		final Key connectionKey = new KeyFactory.Builder(userKey).addChild(getKind(), connectionKeyName).getKey();
		Transaction txn = datastore.beginTransaction();
        boolean updated = false;
		try {			
			Entity entity = datastore.get(txn, connectionKey);
			entity.setProperty("displayName", data.getDisplayName());
			entity.setProperty("profileUrl", data.getProfileUrl());
			entity.setProperty("imageUrl", data.getImageUrl());
			entity.setProperty("accessToken", encrypt(data.getAccessToken()));
			entity.setProperty("secret", encrypt(data.getSecret()));
			entity.setProperty("refreshToken", encrypt(data.getRefreshToken()));
			entity.setProperty("expireTime", data.getExpireTime());			
			datastore.put(txn, entity);
			txn.commit();
            updated = true;
		} catch (EntityNotFoundException e) {
			log.warning("There was the problem updating connection " + connection.getKey().toString() + ". No such connection exists.");
		} finally {
			if (txn.isActive()) {
                txn.rollback();
                updated = false;
            }
		}

        if (updated) {
		    for (ConnectionInterceptor interceptor : interceptingConnectionsTo(connection)) {
			    interceptor.afterUpdate(userId, connection);
		    }
        }
	}


	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void removeConnections(String providerId) {
		final Query query = new Query(getKind())		
			.setAncestor(userKey)
			.setFilter(FilterOperator.EQUAL.of("providerId", providerId));
		Map<Key, Connection<?>> resultMap = DatastoreUtils.queryForMap(
				datastore.prepare(query), connectionMapper);
		Set<Key> keys = resultMap.keySet();
		if (keys.isEmpty()) return;
		
		Collection<Connection<?>> connections = resultMap.values();
		for (ConnectionInterceptor interceptor : interceptingConnectionsTo(connections.iterator().next())) {
			interceptor.beforeRemove(userId, connections);
		}
		
		Transaction txn = datastore.beginTransaction(withXG(true));
		try {
			datastore.delete(txn, keys);
			txn.commit();			
		} finally {
			if (txn.isActive()) txn.rollback();
		}
		for (ConnectionInterceptor interceptor : interceptingConnectionsTo(connections.iterator().next())) {
			interceptor.afterRemove(userId, connections);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void removeConnection(ConnectionKey connectionKey) {
		String connectionKeyName = createConnectionKeyName(userId, connectionKey);
		final Key key = new KeyFactory.Builder(userKey).addChild(getKind(), connectionKeyName).getKey();
		Transaction txn = datastore.beginTransaction();
		try {			
			final Entity entity = datastore.get(txn, key);
			Connection<?> connection = connectionMapper.mapEntity(entity);
			for (ConnectionInterceptor interceptor : interceptingConnectionsTo(connection)) {
				interceptor.beforeRemove(userId, Collections.singletonList(connection));
			}
			datastore.delete(txn, entity.getKey());
			txn.commit();
			for (ConnectionInterceptor interceptor : interceptingConnectionsTo(connection)) {
				interceptor.afterRemove(userId, Collections.singletonList(connection));
			}
		} catch (EntityNotFoundException e) {
			log.warning("There was problem deleted connection " + connectionKey.toString() + ". No such connection exists.");			
		} finally {
			if (txn.isActive()) txn.rollback();
		}		
	}
	
	private final ServiceProviderConnectionMapper connectionMapper = new ServiceProviderConnectionMapper();
	
	private final class ServiceProviderConnectionMapper implements EntityMapper<Connection<?>> {

		@Override
		public Connection<?> mapEntity(Entity entity) {
			ConnectionData connectionData = mapConnectionData(entity);
			ConnectionFactory<?> connectionFactory = connectionFactoryLocator.getConnectionFactory(connectionData.getProviderId());
			return connectionFactory.createConnection(connectionData);			
		}
		
		private ConnectionData mapConnectionData(Entity entity) {
			return new ConnectionData(
				(String) entity.getProperty("providerId"), 
				(String) entity.getProperty("providerUserId"), 
				(String) entity.getProperty("displayName"), 
				(String) entity.getProperty("profileUrl"), 
				(String) entity.getProperty("imageUrl"),
				decrypt((String) entity.getProperty("accessToken")), 
				decrypt((String) entity.getProperty("secret")), 
				decrypt((String) entity.getProperty("refreshToken")), 
				(Long) entity.getProperty("expireTime")
			);
		}
		
		private String decrypt(String encryptedText) {
			return encryptedText != null ? textEncryptor.decrypt(encryptedText) : encryptedText;
		}		
	}
	
	private <A> String getProviderId(Class<A> apiType) {
		return connectionFactoryLocator.getConnectionFactory(apiType).getProviderId();
	}
	
	private String encrypt(String text) {
		return text != null ? textEncryptor.encrypt(text) : text;
	}
	
	public static String createConnectionKeyName(String userId, ConnectionKey connectionKey) {
		return userId + "-" + connectionKey.getProviderId() + "-" + connectionKey.getProviderUserId();
	}
	
	/**
	 * Configure the list of interceptors that should receive callbacks during the connection CRUD operations.
	 * @param interceptors the connect interceptors to add
	 */
	public void setInterceptors(List<ConnectionInterceptor<?>> interceptors) {
		for (ConnectionInterceptor<?> interceptor : interceptors) {
			addInterceptor(interceptor);
		}
	}
	
	/**
	 * Adds a ConnectionInterceptor to receive callbacks during the connection CRUD operations.
	 * @param interceptor the connection interceptor to add
	 */
	public void addInterceptor(ConnectionInterceptor<?> interceptor) {
		Class<?> serviceApiType = GenericTypeResolver.resolveTypeArgument(interceptor.getClass(), ConnectionInterceptor.class);
		interceptors.add(serviceApiType, interceptor);
	}
	
	private List<ConnectionInterceptor<?>> interceptingConnectionsTo(Connection<?> connection) {
		Class<?> serviceType = GenericTypeResolver.resolveTypeArgument(connection.getClass(), Connection.class);
		List<ConnectionInterceptor<?>> typedInterceptors = interceptors.get(serviceType);
		if (typedInterceptors == null) {
			typedInterceptors = Collections.emptyList();
		}
		return typedInterceptors;
	}
		
}
