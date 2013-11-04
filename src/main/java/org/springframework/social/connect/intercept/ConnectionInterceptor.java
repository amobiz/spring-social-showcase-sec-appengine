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
package org.springframework.social.connect.intercept;

import java.util.Collection;

import org.springframework.social.connect.Connection;

/**
 * Listens for the Connection CRUD events. 
 * Allows for custom logic to be executed before and after connections are created/updated/removed
 * @author Vladislav Tserman
 */
public interface ConnectionInterceptor<S> {
	
	/**
	 * Called during connection creation, immediately after new connection is created.
	 * @param userId user id
	 * @param connection the connection that is created
	 */
	public void afterCreate(String userId, Connection<S> connection);
		
	/**
	 * Called during connection creation, right before creating new Connection
	 * @param userId user id
	 * @param connection the connection that is being created
	 */
	public void beforeCreate(String userId, Connection<S> connection);
	
	/**
	 * Called during connections removal, immediately after connections are deleted.
	 * @param userId user id
	 * @param connections list of connections that are removed
	 */
	public void afterRemove(String userId, Collection<? extends Connection<S>> connections);
		
	/**
	 * Called during connections removal, right before connections are deleted.
	 * @param userId user id
	 * @param connections list of connections that are being deleted
	 */
	public void beforeRemove(String userId, Collection<? extends Connection<S>> connections);
		
	/**
	 * Called during connection updating, immediately after the connection is updated.
	 * @param userId user id
	 * @param connection the connection that is updated
	 */
	public void afterUpdate(String userId, Connection<S> connection);
	
	/**
	 * Called during connection updating, right before updating Connection
	 * @param userId user id
	 * @param connection the connection that is being updated
	 */
	public void beforeUpdate(String userId, Connection<S> connection);
}
