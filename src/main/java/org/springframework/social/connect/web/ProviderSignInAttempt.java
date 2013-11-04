/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.social.connect.web;

import java.io.Serializable;

import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionData;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.DuplicateConnectionException;
import org.springframework.social.connect.UsersConnectionRepository;


/**
 * Models an attempt to sign-in to the application using a provider user identity.
 * Instances are created when the provider sign-in process could not be completed because no local user is associated with the provider user.
 * This could happen because the user has not yet signed up with the application, or has not yet connected their local application identity with the their provider identity.
 * For the former scenario, callers should invoke {@link #addConnection(String)} post-signup to establish a connection between a new user account and the provider account.
 * For the latter, existing users should sign-in using their local application credentials and formally connect to the provider they also wish to authenticate with.
 * @author Keith Donald
 *
 * Note on my dirty hack：
 *
 * Changes the references to non-serializable objects transient.
 *
 * Issue Tracker:
 *
 * ProviderSignInAttempt - NotSerializableException: ConnectionFactoryRegistry
 * https://jira.springsource.org/browse/SOCIAL-203?page=com.atlassian.jira.plugin.system.issuetabpanels:all-tabpanel
 *
 */
@SuppressWarnings("serial")
public class ProviderSignInAttempt implements Serializable {

    /**
     * Name of the session attribute ProviderSignInAttempt instances are indexed under.
     */
    public static final String SESSION_ATTRIBUTE = ProviderSignInAttempt.class.getName();

    private final ConnectionData connectionData;

    // Dirty hack: make this transient
    private final transient ConnectionFactoryLocator connectionFactoryLocator;

    // Dirty hack: make this transient
    private final transient UsersConnectionRepository connectionRepository;

    public ProviderSignInAttempt(Connection<?> connection, ConnectionFactoryLocator connectionFactoryLocator, UsersConnectionRepository connectionRepository) {
        this.connectionData = connection.createData();
        this.connectionFactoryLocator = connectionFactoryLocator;
        this.connectionRepository = connectionRepository;
    }

    /**
     * Get the connection to the provider user account the client attempted to sign-in as.
     * Using this connection you may fetch a {@link Connection#fetchUserProfile() provider user profile} and use that to pre-populate a local user registration/signup form.
     * You can also lookup the id of the provider and use that to display a provider-specific user-sign-in-attempt flash message e.g. "Your Facebook Account is not connected to a Local account. Please sign up."
     */
    public Connection<?> getConnection() {
        return connectionFactoryLocator.getConnectionFactory(connectionData.getProviderId()).createConnection(connectionData);
    }

    /**
     * Connect the new local user to the provider.
     * @throws DuplicateConnectionException if the user already has this connection
     */
    void addConnection(String userId) {
        connectionRepository.createConnectionRepository(userId).addConnection(getConnection());
    }

}
