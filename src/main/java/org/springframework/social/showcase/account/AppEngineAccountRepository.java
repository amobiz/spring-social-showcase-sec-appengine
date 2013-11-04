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
package org.springframework.social.showcase.account;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.logging.Logger;

@Repository
public class AppEngineAccountRepository implements AccountRepository {

    private final static Logger log = Logger.getLogger( AppEngineAccountRepository.class.getName() );
    private final DatastoreService db;

	public AppEngineAccountRepository() {
        this.db = DatastoreServiceFactory.getDatastoreService();
	}

	@Transactional
	public void createAccount(Account user) throws UsernameAlreadyInUseException {
        log.info( "createAccount: user=" + user );
        Key k = KeyFactory.createKey( "account", user.getUsername() );
        try {
            db.get( k );
            throw new UsernameAlreadyInUseException( "" );
        }
        catch ( EntityNotFoundException ex ) {
        }
        Entity e = new Entity( "account", user.getUsername() );
        e.setProperty( "firstName", user.getFirstName() );
        e.setProperty( "lastName", user.getLastName() );
        e.setProperty( "username", user.getUsername() );
        e.setProperty( "password", user.getPassword() );
        db.put(e);
	}

	public Account findAccountByUsername(String username) {
        log.info( "findAccountByUsername: username=" + username );
        Key k = KeyFactory.createKey( "account", username );
        try {
            Entity e = db.get( k );
            return new Account( (String)e.getProperty("username"),
                    null,
                    (String)e.getProperty("firstName"),
                    (String)e.getProperty("lastName")
            );
        }
        catch ( EntityNotFoundException ex ) {
            return null;
        }

        /*
        Query q = new Query( "account" );
        q.setFilter( new Query.FilterPredicate( "username", FilterOperator.EQUAL, username ) );
        PreparedQuery pq = db.prepare( q );
        Entity e = pq.asSingleEntity();
        return new Account( (String)e.getProperty("username"),
                null,
                (String)e.getProperty("firstName"),
                (String)e.getProperty("lastName")
                );
        */
	}

}
